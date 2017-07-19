/*******************************************************************************
 * Copyright (C) 2015, 2016 RAPID EU Project
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *******************************************************************************/
package eu.project.rapid.as;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;
import eu.project.rapid.ac.ResultContainer;
import eu.project.rapid.ac.profilers.DeviceProfiler;
import eu.project.rapid.ac.utils.Utils;
import eu.project.rapid.common.Configuration;
import eu.project.rapid.common.RapidConstants;
import eu.project.rapid.common.RapidMessages;
import eu.project.rapid.common.RapidUtils;

/**
 * The server-side class handling client requests for invocation. Works in a separate thread for
 * each client. This can handle requests coming from the phone (when behaving as the main vm) or
 * requests coming from the main vm when behaving as a vm helper.
 * <p>
 * FIXME: When running the AS on Android-x86 6.0 the native libraries are correctly loaded.
 * On Android 4.4 it doesn't work properly.
 */
public class AppHandler implements Runnable {

    private static AtomicInteger id = new AtomicInteger();
    private String TAG;

    private Configuration config;
    // The main thread has vmId = 0
    // the vm helpers have vmId \in [1, nrVMs-1]
    private int vmHelperId = 0;
    private final Socket mClient;
    private DynamicObjectInputStream mObjIs;
    private ObjectOutputStream mObjOs;
    private final Context mContext;

    // The number of vm helpers requested (not considering the main VM)
    private static int numberOfVMHelpers = 0;
    private Boolean[] pausedHelper; // Needed for synchronization with the vm helpers
    private int requestFromMainServer = 0; // The main vm sends commands to the vm helpers
    private Object responsesFromServers; // Array of partial results returned by the vm helpers
    // The main thread waits for all the vm helpers to finish execution
    private final AtomicInteger nrVMsReady = new AtomicInteger(0);

    private static Map<String, Integer> apkMap = new ConcurrentHashMap<>(); // appName, apkSize
    private static Map<String, CountDownLatch> apkMapSemaphore = new ConcurrentHashMap<>(); // appName, latch
    private String appName; // the app name sent by the phone
    private int appLength; // the app length in bytes sent by the phone
    private Object objToExecute = new Object(); // the object to be executed sent by the phone
    private String methodName; // the method to be executed
    private Class<?>[] pTypes; // the types of the parameters passed to the method
    private Object[] pValues; // the values of the parameters to be passed to the method
    private Class<?> returnType; // the return type of the method
    private String apkFilePath; // the path where the apk is installed
    // I need this variable for when the DS starts the VM migration.
    // The AS will let the DS shut down the VM only if this is equal to 0.
    private static final AtomicInteger nrTasksCurrentlyBeingExecuted = new AtomicInteger(0);
    private static AtomicBoolean migrationInProgress = new AtomicBoolean(false);

    /**
     * Key is the appName.
     * Value is a map <libName, File> keeping a map of files pointing to the library.
     * Static because more than one thread will be launched to deal with one app.
     * They can rename the library (by adding an increasing index number), so is important
     * that all threads use the same library then,
     * otherwise one can end up not finding the library anymore at all.
     */
    private List<File> libraries = new LinkedList<>();
    private static Map<String, Map<String, Integer>> librariesIndex = new ConcurrentHashMap<>();

    // For parallelization
    private Socket dsSocket = null;
    private OutputStream dsOs;
    private InputStream dsIs;
    private ObjectOutputStream dsOos = null;
    private ObjectInputStream dsOis = null;
    private ArrayList<String> vmHelperIPs;

    // Classloaders needed by the dynamicObjectInputStream
    private ClassLoader mCurrent = ClassLoader.getSystemClassLoader();
    private DexClassLoader mCurrentDexLoader;

    public AppHandler(Socket pClient, final Context cW, Configuration config) {
        TAG = AppHandler.class.getName() + "-" + id.getAndIncrement();
        Log.d(TAG, "New Client connected");
        this.mClient = pClient;
        this.mContext = cW;
        this.config = config;
    }

    @Override
    public void run() {

        try {
            InputStream mIs = mClient.getInputStream();
            OutputStream mOs = mClient.getOutputStream();
            mObjIs = new DynamicObjectInputStream(mIs);
            mObjOs = new ObjectOutputStream(mOs);

            mObjIs.setClassLoaders(mCurrent, mCurrentDexLoader);

            int request = 0;
            while (request != -1) {

                request = mIs.read();
                Log.d(TAG, "Request - " + request);

                switch (request) {
                    case RapidMessages.AC_OFFLOAD_REQ_AS:

                        Log.v(TAG, "Got a new request for execution");
                        synchronized (nrTasksCurrentlyBeingExecuted) {
                            if (migrationInProgress.get()) {
                                Log.w(TAG, "VM upgrade in progress, cannot accept new tasks");
                                // Simply closing the connection will force the client to run tasks locally
                                closeConnection();
                                break;
                            }

                            nrTasksCurrentlyBeingExecuted.incrementAndGet();
                            Log.v(TAG, "The new task is accepted for execution, total nr of tasks: "
                                    + nrTasksCurrentlyBeingExecuted.get());
                        }

                        // Start profiling on remote side
                        DeviceProfiler devProfiler = new DeviceProfiler(mContext);
                        devProfiler.startDeviceProfiling();

                        Log.d(TAG, "Execute request - " + request);
                        Object result = retrieveAndExecute(mObjIs);

                        // Delete the file containing the vmHelperId assigned to this vm
                        // (if such file does not exist do nothing)
                        Utils.deleteCloneHelperId();

                        devProfiler.stopAndCollectDeviceProfiling();

                        try {
                            // Send back over the socket connection

                            // RapidUtils.sendAnimationMsg(config, RapidMessages.AC_RESULT_REMOTE);
                            mObjOs.writeObject(result);
                            // Clear ObjectOutputCache - Java caching unsuitable in this case
                            mObjOs.flush();
                            mObjOs.reset();

                            Log.d(TAG, "Result successfully sent");

                        } catch (IOException e) {
                            Log.d(TAG, "Connection failed");
                            e.printStackTrace();
                            return;
                        } finally {
                            synchronized (nrTasksCurrentlyBeingExecuted) {
                                nrTasksCurrentlyBeingExecuted.decrementAndGet();
                                nrTasksCurrentlyBeingExecuted.notifyAll();
                            }
                        }

                        break;

                    case RapidMessages.PING:
                        Log.d(TAG, "Reply to PING");
                        mOs.write(RapidMessages.PONG);
                        break;

                    case RapidMessages.AC_REGISTER_AS:
                        Log.d(TAG, "Registering apk");
                        appName = (String) mObjIs.readObject();
                        Log.i(TAG, "apk name: " + appName);

                        appLength = mObjIs.readInt();
                        apkFilePath = mContext.getFilesDir().getAbsolutePath() + "/" + appName + ".apk";
                        Log.d(TAG, "Registering apk: " + appName + " of size: " + appLength + " bytes");
                        if (apkPresent(apkFilePath, appName, appLength)) {
                            Log.d(TAG, "APK present");
                            mOs.write(RapidMessages.AS_APP_PRESENT_AC);
                        } else {
                            Log.d(TAG, "Request APK");
                            mOs.write(RapidMessages.AS_APP_REQ_AC);
                            // Receive the apk file from the client
                            receiveApk(mIs, mObjIs, apkFilePath);

                            // Delete the old .dex file of this apk to avoid the crash due to dexopt:
                            // DexOpt: source file mod time mismatch (457373af vs 457374dd)
                            // D/dalvikvm( 3885): ODEX file is stale or bad; removing and retrying
                            String oldDexFilePath =
                                    mContext.getFilesDir().getAbsolutePath() + "/" + appName + ".dex";
                            if (!new File(oldDexFilePath).delete()) {
                                Log.v(TAG, "Could not delete the old Dex file: " + oldDexFilePath);
                            }
                            apkMapSemaphore.get(appName).countDown();
                        }

                        // Wait for the file to be written on the disk
                        apkMapSemaphore.get(appName).await();
                        // Create the new (if needed) dex file and load the .dex file
                        File dexFile = new File(apkFilePath);
                        Log.d(TAG, "APK file size on disk: " + dexFile.length());
                        addLibraries(dexFile);
                        mCurrentDexLoader = mObjIs.addDex(dexFile);
                        Log.d(TAG, "DEX file added.");

                        break;

                    case RapidMessages.CLONE_ID_SEND:
                        vmHelperId = mIs.read();
                        Utils.writeCloneHelperId(vmHelperId);
                        break;

                    case RapidMessages.DS_MIGRATION_VM_AS:
                        // When the QoS are not respected, the VM will be upgraded.
                        // In that case, the DS informs the AS, so that the AS can inform the AC.
                        long userId = mObjIs.readLong();
                        Log.w(TAG, "The VMM is informing that there will be a VM migration/update, user id = " + userId);
                        migrationInProgress.getAndSet(true);

                        synchronized (nrTasksCurrentlyBeingExecuted) {
                            while (nrTasksCurrentlyBeingExecuted.get() > 0) {
                                try {
                                    nrTasksCurrentlyBeingExecuted.wait();
                                } catch (Exception e) {
                                    Log.v(TAG, "Exception while waiting for no more tasks in execution: " + e);
                                }
                            }
                        }
                        Log.v(TAG, "No more tasks under execution, informing the VMM that the upgrade can go on...");
                        mObjOs.writeByte(RapidMessages.OK);
                        mObjOs.flush();
                        break;
                }
            }
            Log.d(TAG, "Client disconnected");
        } catch (SocketException e) {
            Log.w(TAG, "Client disconnected: " + e);
        } catch (Exception e) {
            // We don't want any exceptions to escape from here,
            // hide everything silently if we didn't foresee them cropping
            // up... Since we don't want the server to die because
            // somebody's program is misbehaving
            Log.e(TAG, "Client disconnected: " + e);
//            e.printStackTrace();
        } catch (Error e) {
            // We don't want any exceptions to escape from here,
            // hide everything silently if we didn't foresee them dropping
            // up... Since we don't want the server to die because
            // somebody's program is misbehaving
            Log.e(TAG, "Error not caught properly - " + e);
            e.printStackTrace();
        } finally {
            closeConnection();
            deleteLibFiles();
        }
    }

    private void closeConnection() {
        RapidUtils.closeQuietly(mObjOs);
        RapidUtils.closeQuietly(mObjIs);
        RapidUtils.closeQuietly(mClient);
    }

    /**
     * Delete the library files
     */
    private void deleteLibFiles() {

        for (File f : libraries) {
            File fParent = f.getParentFile();
            if (fParent.isDirectory()) {
                for (File f2 : fParent.listFiles()) {
                    if (!f2.delete()) {
                        Log.v(TAG, "Not possible to delete library file: " + f2);
                    }
                }
            }
            // Do not delete the parent, needed for the library index.
//                fParent.delete();
        }
    }

    /**
     * Method to retrieve an apk of an application that needs to be executed
     *
     * @param objIn Object input stream to simplify retrieval of data
     */
    private void receiveApk(InputStream is, DynamicObjectInputStream objIn, String apkFilePath) {
        // Receiving the apk file
        // Get the length of the file receiving
        try {
            // Write it to the filesystem
            File apkFile = new File(apkFilePath);
            FileOutputStream fout = new FileOutputStream(apkFile);
            BufferedOutputStream bout = new BufferedOutputStream(fout, RapidConstants.BUFFER_SIZE_ANDROID);

            // Get the apk file
            Log.d(TAG, "Starting reading apk file of size: " + appLength + " bytes");
            byte[] tempArray = new byte[RapidConstants.BUFFER_SIZE_ANDROID];
            int read;
            int totalRead = 0;
            int prevPerc = 0;
            int currPerc;
            while (totalRead < appLength) {
//                read = objIn.read(tempArray);
                read = is.read(tempArray);
                totalRead += read;
                // Log.d(TAG, "Read " + read + " bytes");
                bout.write(tempArray, 0, read);

                currPerc = (int) (((double) totalRead / appLength) * 100);
                if (currPerc % 10 > prevPerc) {
                    Log.d(TAG, "Got: " + currPerc + " % of the apk.");
                    Log.d(TAG, "TotalRead: " + totalRead + " of " + appLength + " bytes");
                    prevPerc = currPerc;
                }
            }

            bout.flush();
            bout.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Extract native libraries for the x86/x86_64 platform included in the .apk file (which is actually a
     * zip file).
     * <p>
     * The x86/x86_64 shared libraries are: lib/[x86\x86_64]/library.so inside the apk file. We extract them from the
     * apk and save in the /data/data/eu.rapid.project.as/files folder. Initially we used to save them
     * with the same name as the original (library.so) but this caused many problems related to
     * classloaders. When an app was offloaded for the first time and used the library, the library
     * was loaded in the jvm. If the client disconnected, the classloader that loaded the library was
     * not unloaded, which means that also the library was not unloaded from the jvm. On consequent
     * offloads of the same app, the classloader is different, meaning that the library could not be
     * loaded anymore due to the fact that was already loaded by another classloader. But it could not
     * even be used, due to the fact that the classloaders differ.<br>
     * <br>
     * To solve this problem we save the library within a new folder, increasing a sequence number
     * each time the same app is offloaded. So, the library.so file will be saved as
     * library-1/library.so, library-2/library.so, and so on.
     *
     * @param dexFile the apk file
     */

    @SuppressWarnings("unchecked")
    private synchronized void addLibraries(File dexFile) {
        Long startTime = System.nanoTime();

        ZipFile apkFile;
//        LinkedList<File> libFiles = new LinkedList<>();
        try {
            apkFile = new ZipFile(dexFile);
            Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) apkFile.entries();
            ZipEntry entry;
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                // Zip entry for a lib file is in the form of
                // lib/platform/library.so
                // But only load x86/x86_64 libraries on the server side
                if (entry.getName().matches("lib/" + AccelerationServer.arch + "/(.*).so")) {
                    Log.d(TAG, "Matching APK entry - " + entry.getName());
                    // Unzip the lib file from apk
                    BufferedInputStream is = new BufferedInputStream(apkFile.getInputStream(entry));

                    // Folder where to put the libraries (usually this will resolve to:
                    // /data/data/eu.rapid.project.as/files)
                    File libFolder = new File(mContext.getFilesDir().getAbsolutePath());

                    // Get the library name without the .so extension
                    final String libName = entry.getName().replace("lib/" +
                            AccelerationServer.arch + "/", "").replace(".so", "");

                    if (!librariesIndex.containsKey(appName)) {
                        librariesIndex.put(appName, new HashMap<String, Integer>());
                    }

                    // The sequence number to append to the library name
                    int libSeqNr = 0;
                    if (librariesIndex.get(appName).containsKey(libName)) {
                        libSeqNr = librariesIndex.get(appName).get(libName);
                    } else {
                        for (File f : libFolder.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File file, String s) {
                                return s.matches(libName + "-\\d+");
                            }
                        })) {
                            // Scan all the previously created folder libraries
                            int lastIndexDash = f.getName().lastIndexOf("-");
                            if (lastIndexDash != -1) {
                                try {
                                    libSeqNr = Math.max(libSeqNr, Integer.parseInt(f.getName().substring(lastIndexDash + 1)));
                                } catch (Exception e) {
                                    Log.w(TAG,
                                            "Library file does not contain any number in the name, maybe is not written by us!");
                                }
                            }
                        }
                    }

                    libSeqNr++;
                    librariesIndex.get(appName).put(libName, libSeqNr);

                    File currLibFolder =
                            new File(libFolder.getAbsolutePath() + File.separator + libName + "-" + libSeqNr);
                    if (!currLibFolder.mkdir()) {
                        Log.e(TAG, "Could not create folder: " + currLibFolder);
                    }

                    File libFile =
                            new File(currLibFolder.getAbsolutePath() + File.separator + libName + ".so");
                    if (!libFile.createNewFile()) {
                        Log.e(TAG, "Could not create file: " + libFile);
                    }

                    Log.d(TAG, "Writing lib file to " + libFile.getAbsolutePath());
                    FileOutputStream fos = new FileOutputStream(libFile);
                    BufferedOutputStream dest = new BufferedOutputStream(fos, RapidConstants.BUFFER_SIZE_ANDROID);

                    byte data[] = new byte[RapidConstants.BUFFER_SIZE_ANDROID];
                    int count;
                    while ((count = is.read(data, 0, RapidConstants.BUFFER_SIZE_ANDROID)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                    is.close();

                    // Store the library on the map
                    libraries.add(libFile);
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "ERROR: File unzipping error " + e);
        }
        Log.d(TAG,
                "Duration of unzipping libraries - " + ((System.nanoTime() - startTime) / 1000000) + "ms");
    }

    /**
     * Reads in the object to execute an operation on, name of the method to be executed and executes
     * it
     *
     * @param objIn Dynamic object input stream for reading an arbitrary object (class loaded from a
     *              previously obtained dex file inside an apk)
     * @return result of executing the required method
     * @throws IllegalArgumentException
     * @throws SecurityException
     */
    private Object retrieveAndExecute(DynamicObjectInputStream objIn) {
        Long getObjectDuration = -1L;
        Long startTime = System.nanoTime();
        // Read the object in for execution
        Log.d(TAG, "Read Object");
        try {

            // Receive the number of VMs needed
            numberOfVMHelpers = objIn.readInt();
            Log.i(TAG, "The user is asking for " + numberOfVMHelpers + " VMs");
            numberOfVMHelpers--;
            boolean withMultipleVMs = numberOfVMHelpers > 0;

            // Get the object
            objToExecute = objIn.readObject();

            // Get the class of the object, dynamically
            Class<?> objClass = objToExecute.getClass();

            getObjectDuration = System.nanoTime() - startTime;

            Log.d(TAG, "Done Reading Object: " + objClass.getName() + " in "
                    + (getObjectDuration / 1000000000.0) + " seconds");

            // Set up server-side DFE for the object
            java.lang.reflect.Field fDfe = objClass.getDeclaredField("dfe");
            fDfe.setAccessible(true);

            Class<?> dfeType = fDfe.getType();
            Constructor<?> cons = dfeType.getDeclaredConstructor();
            cons.setAccessible(true);
            Object dfe = null;
            try {
                dfe = cons.newInstance();
            } catch (InstantiationException e) {
                // too bad. still try to carry on.
                e.printStackTrace();
            }

            fDfe.set(objToExecute, dfe);

            Log.d(TAG, "Read Method");
            // Read the name of the method to be executed
            methodName = (String) objIn.readObject();

            Object tempTypes = objIn.readObject();
            pTypes = (Class[]) tempTypes;

            Object tempValues = objIn.readObject();
            pValues = (Object[]) tempValues;

            Log.d(TAG, "Run Method " + methodName);
            // Get the method to be run by reflection
            Method runMethod = objClass.getDeclaredMethod(methodName, pTypes);
            // And force it to be accessible (quite often would be declared
            // private originally)
            runMethod.setAccessible(true); // Set the method to be accessible

            if (withMultipleVMs) {
                pausedHelper = new Boolean[numberOfVMHelpers + 1];
                for (int i = 1; i < numberOfVMHelpers + 1; i++) {
                    pausedHelper[i] = true;
                }

                withMultipleVMs = askDsForVMHelpers();

                if (withMultipleVMs) {
                    Log.i(TAG, "The VMs are successfully allocated.");
                    dsOos.writeByte(RapidMessages.PARALLEL_START);
                    dsOos.flush();

                    // Assign the IDs to the new vm helpers
                    Log.i(TAG, "The helper VMs:");
                    int vmHelperId = 1;
                    for (String vmHelperIp : vmHelperIPs) {
                        Log.i(TAG, vmHelperIp);
                        // Start the thread that should connect to the vm helper
                        (new VMHelperThread(vmHelperId++, vmHelperIp)).start();
                    }

                    returnType = runMethod.getReturnType(); // the return type of the offloaded method

                    // Allocate the space for the responses from the other VMs
                    responsesFromServers = Array.newInstance(returnType, numberOfVMHelpers + 1);

                    // Wait until all the threads are connected to the vm helpers
                    waitForVMHelpersToBeReady();

                    // Give the command to register the app first
                    sendCommandToAllHelperThreads(RapidMessages.AC_REGISTER_AS);

                    // Wait again for the threads to be ready
                    waitForVMHelpersToBeReady();

                    // And send a ping to all VMs just for testing
                    // sendCommandToAllHelperThreads(RapidMessages.PING);
                    // waitForVMHelpersToBeReady();

                    // Wake up the server helper threads and tell them to send the object to execute, the
                    // method, parameter types and parameter values

                    sendCommandToAllHelperThreads(RapidMessages.AC_OFFLOAD_REQ_AS);
                } else {
                    Log.i(TAG, "Could not allocate other VMs, doing only my part of the job.");
                }
            }

            // Run the method and retrieve the result
            Object result;
            Long execDuration = null;
            try {
                // RapidUtils.sendAnimationMsg(config, RapidMessages.AC_EXEC_REMOTE);
                Long startExecTime = System.nanoTime();
                try {
                    // long s = System.nanoTime();
                    Method prepareDataMethod =
                            objToExecute.getClass().getDeclaredMethod("prepareDataOnServer");
                    prepareDataMethod.setAccessible(true);
                    // RapidUtils.sendAnimationMsg(config, RapidMessages.AC_PREPARE_DATA);
                    long s = System.nanoTime();
                    prepareDataMethod.invoke(objToExecute);
                    long prepareDataDuration = System.nanoTime() - s;
                    Log.w(TAG, "Executed method prepareDataOnServer() on " + (prepareDataDuration / 1000000)
                            + " ms");

                } catch (NoSuchMethodException e) {
                    Log.w(TAG, "The method prepareDataOnServer() does not exist");
                }

                result = runMethod.invoke(objToExecute, pValues);
                execDuration = System.nanoTime() - startExecTime;
                Log.d(TAG,
                        runMethod.getName() + ": pure execution time - " + (execDuration / 1000000) + "ms");
            } catch (InvocationTargetException e) {
                // The method might have failed if the required shared library
                // had not been loaded before, try loading the apk's libraries and
                // restarting the method
                if (e.getTargetException() instanceof UnsatisfiedLinkError
                        || e.getTargetException() instanceof ExceptionInInitializerError) {
                    Log.d(TAG, "UnsatisfiedLinkError thrown, loading libs and retrying");

                    Method libLoader = objClass.getMethod("loadLibraries", LinkedList.class);
                    try {
                        libLoader.invoke(objToExecute, libraries);
                        // RapidUtils.sendAnimationMsg(config, RapidMessages.AC_EXEC_REMOTE);
                        Long startExecTime = System.nanoTime();
                        result = runMethod.invoke(objToExecute, pValues);
                        execDuration = System.nanoTime() - startExecTime;
                        Log.d(TAG,
                                runMethod.getName() + ": pure execution time - " + (execDuration / 1000000) + "ms");
                    } catch (InvocationTargetException e1) {
                        Log.e(TAG, "InvocationTargetException after loading the libraries");
                        result = e1;
                        e1.printStackTrace();
                    }
                } else {
                    Log.w(TAG, "The remote execution resulted in exception:  " + e);
                    result = e;
                    e.printStackTrace();
                }
            }

            Log.d(TAG, runMethod.getName() + ": retrieveAndExecute time - "
                    + ((System.nanoTime() - startTime) / 1000000) + "ms");

            if (withMultipleVMs) {
                // Wait for all the VMs to finish execution before returning the result
                waitForVMHelpersToBeReady();
                Log.d(TAG, "All servers finished execution, send result back.");

                // Kill the threads.
                sendCommandToAllHelperThreads(-1);

                // put the result of the main vm as the first element of the array
                synchronized (responsesFromServers) {
                    Array.set(responsesFromServers, 0, result);
                }

                // Call the reduce function implemented by the developer to combine the partial results.
                try {
                    // Array of the returned type
                    Class<?> arrayReturnType =
                            Array.newInstance(returnType, numberOfVMHelpers + 1).getClass();
                    Method runMethodReduce =
                            objClass.getDeclaredMethod(methodName + "Reduce", arrayReturnType);
                    runMethodReduce.setAccessible(true);
                    Log.i(TAG, "Reducing the results using the method: " + runMethodReduce.getName());

                    Object reducedResult =
                            runMethodReduce.invoke(objToExecute, new Object[]{responsesFromServers});
                    result = reducedResult;

                    Log.i(TAG, "The reduced result: " + reducedResult);

                } catch (Exception e) {
                    Log.e(TAG, "Impossible to reduce the result");
                    e.printStackTrace();
                }

                dsOos.writeByte(RapidMessages.PARALLEL_END);
                dsOos.flush();
            }

            // If this is the main vm send back also the object to execute,
            // otherwise the helper VMs don't need to send it back.
            if (vmHelperId == 0) {
                return new ResultContainer(objToExecute, result, getObjectDuration, execDuration);
            } else {
                return new ResultContainer(null, result, getObjectDuration, execDuration);
            }

        } catch (Exception e) {
            // catch and return any exception since we do not know how to handle
            // them on the server side
            e.printStackTrace();
            return new ResultContainer(e, getObjectDuration);
        } finally {
            RapidUtils.closeQuietly(dsOis);
            RapidUtils.closeQuietly(dsOos);
            RapidUtils.closeQuietly(dsSocket);
        }
    }

    private void waitForVMHelpersToBeReady() {
        // Wait for the threads to be ready
        synchronized (nrVMsReady) {
            while (nrVMsReady.get() < numberOfVMHelpers) {
                try {
                    nrVMsReady.wait();
                } catch (InterruptedException e) {
                    Log.v(TAG, "Thread wait() was interrupted, putting back to sleep...");
                }
            }

            nrVMsReady.set(0);
        }
    }

    private void sendCommandToAllHelperThreads(int command) {
        synchronized (pausedHelper) {
            for (int i = 1; i < numberOfVMHelpers + 1; i++) {
                pausedHelper[i] = false;
            }
            requestFromMainServer = command;
            pausedHelper.notifyAll();
        }
    }

    /**
     * Check if the application is already present on the machine
     *
     * @param filename filename of the apk file (used for identification)
     * @return true if the apk is present, false otherwise
     */
    private synchronized boolean apkPresent(String filename, String appName, int appLength) {
        // TODO: more sophisticated checking for existence
        File apkFile = new File(filename);

        if (apkFile.exists() && apkFile.length() == appLength) {
            apkMapSemaphore.put(appName, new CountDownLatch(0));
            return true;
        }

        if (apkMap.get(appName) == null || apkMap.get(appName) != appLength) {
            apkMap.put(appName, appLength);
            apkMapSemaphore.put(appName, new CountDownLatch(1));
            return false;
        }

        return true;
    }

    /**
     * Connect to the DS and ask for more VMs.<br>
     * The DS will reply with the IP address of the VMs<br>
     * <p>
     * Launch the threads to connect to the other VMs.<br>
     */
    private boolean askDsForVMHelpers() {
        try {
            Log.d(TAG, "Trying to connect to the DS - " + config.getDSIp() + ":" + config.getDSPort());

            // Connect to the directory service
            dsSocket = new Socket(config.getDSIp(), config.getDSPort());
            dsOs = dsSocket.getOutputStream();
            dsIs = dsSocket.getInputStream();

            dsOos = new ObjectOutputStream(dsOs);
            dsOis = new ObjectInputStream(dsIs);

            Log.d(TAG, "Connection established with the DS - " + config.getDSIp() + ":"
                    + config.getDSPort());

            // Ask for helper VMs
            dsOos.writeByte(RapidMessages.PARALLEL_REQ);
            dsOos.writeLong(AccelerationServer.vmId);
            dsOos.writeInt(numberOfVMHelpers);
            dsOos.flush();

            vmHelperIPs = (ArrayList<String>) dsOis.readObject();
            if (vmHelperIPs.size() != numberOfVMHelpers) {
                Log.i(TAG, "The DS could not start the needed VMs, actually started: "
                        + vmHelperIPs.size());
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Exception connecting to the DS: " + e);
        } catch (Error e) {
            Log.e(TAG, "Error connecting to the DS: " + e);
        }

        return false;
    }


    /**
     * The thread taking care of communication with the VM helpers
     */
    private class VMHelperThread extends Thread {

        private String TAG = "ServerHelper-";
        String vmHelperIp;
        private Socket mSocket;
        private OutputStream mOutStream;
        private InputStream mInStream;
        private ObjectOutputStream mObjOutStream;
        private DynamicObjectInputStream mObjInStream;

        // This id is assigned to the vm helper by the main vm.
        // It is needed for splitting the input when parallelizing a certain method (see for example
        // virusScanning).
        // To not be confused with the id that the AS has read from the config file.
        private int vmHelperId;

        VMHelperThread(int vmHelperId, String vmHelperIp) {
            this.vmHelperId = vmHelperId;
            this.vmHelperIp = vmHelperIp;
            TAG = TAG + this.vmHelperId;
        }

        @Override
        public void run() {

            try {

                // Try to connect to the VM helper.
                // If it is not possible to connect stop running.
                if (!establishConnection()) {
                    // Try to close created sockets
                    closeConnection();
                    return;
                }

                // Send the vmId to this vm.
                mOutStream.write(RapidMessages.CLONE_ID_SEND);
                mOutStream.write(vmHelperId);

                while (true) {

                    synchronized (nrVMsReady) {
                        Log.d(TAG, "Server Helpers started so far: " + nrVMsReady.addAndGet(1));
                        if (nrVMsReady.get() >= AppHandler.numberOfVMHelpers)
                            nrVMsReady.notifyAll();
                    }

                    // wait() until the main server wakes up the thread then do something depending on the
                    // request
                    synchronized (pausedHelper) {
                        while (pausedHelper[vmHelperId]) {
                            try {
                                pausedHelper.wait();
                            } catch (InterruptedException e) {
                                Log.v(TAG, "Thread wait() was interrupted, putting back to sleep");
                            }
                        }

                        pausedHelper[vmHelperId] = true;
                    }

                    Log.d(TAG, "Sending command: " + requestFromMainServer);

                    switch (requestFromMainServer) {

                        case RapidMessages.PING:
                            pingOtherServer();
                            break;

                        case RapidMessages.AC_REGISTER_AS:
                            mOutStream.write(RapidMessages.AC_REGISTER_AS);
                            mObjOutStream.writeObject(appName);
                            mObjOutStream.writeInt(appLength);
                            mObjOutStream.flush();

                            int response = mInStream.read();

                            if (response == RapidMessages.AS_APP_REQ_AC) {
                                // Send the APK file if needed
                                Log.d(TAG, "Sending apk to the VM Helper " + vmHelperIp);

                                File apkFile = new File(apkFilePath);
                                FileInputStream fin = new FileInputStream(apkFile);
                                BufferedInputStream bis = new BufferedInputStream(fin);
                                byte[] tempArray = new byte[RapidConstants.BUFFER_SIZE_ANDROID];
                                int read;
                                Log.d(TAG, "Sending apk");
                                while ((read = bis.read(tempArray, 0, tempArray.length)) > -1) {
                                    mOutStream.write(tempArray, 0, read);
//                                    mObjOutStream.write(tempArray, 0, read);
                                }
                                Log.d(TAG, "Finished sending the apk!");
//                                mObjOutStream.flush();
//                                Log.d(TAG, "Finished sending the apk");
                                bis.close();
                            } else if (response == RapidMessages.AS_APP_PRESENT_AC) {
                                Log.d(TAG, "Application already registered on VM " + vmHelperIp);
                            }
                            break;

                        case RapidMessages.AC_OFFLOAD_REQ_AS:
                            Log.d(TAG, "Asking VM Helper " + vmHelperIp + " to parallelize the execution");

                            mOutStream.write(RapidMessages.AC_OFFLOAD_REQ_AS);

                            // Send the number of VMs needed.
                            // Since this is a helper VM, only one vm should be requested.
                            mObjOutStream.writeInt(1);
                            mObjOutStream.writeObject(objToExecute);
                            mObjOutStream.writeObject(methodName);
                            mObjOutStream.writeObject(pTypes);
                            mObjOutStream.writeObject(pValues);
                            mObjOutStream.flush();

                            // This is the response from the vm helper, which is a partial result of the method
                            // execution. This partial result is stored in an array, and will be later composed
                            // with the other partial results of the other VMs to obtain the total desired
                            // result to be sent back to the phone.
                            Object vmHelperResult = mObjInStream.readObject();

                            ResultContainer container = (ResultContainer) vmHelperResult;

                            Log.d(TAG, "Received response from vm ip: " + vmHelperResult + " port: "
                                    + config.getClonePort());
                            Log.d(TAG, "Writing in responsesFromServer in position: " + vmHelperId);
                            synchronized (responsesFromServers) {
                                Array.set(responsesFromServers, vmHelperId, container.functionResult);
                            }
                            break;

                        case -1:
                            closeConnection();
                            return;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e);
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "ClassNotFoundException: " + e);
            } finally {
                closeConnection();
            }
        }

        private boolean establishConnection() {
            try {

                Log.d(TAG, "Trying to connect to vm " + vmHelperIp + ":" + config.getClonePort());

                mSocket = new Socket();
                mSocket.connect(new InetSocketAddress(vmHelperIp, config.getClonePort()), 5 * 1000);

                mOutStream = mSocket.getOutputStream();
                mInStream = mSocket.getInputStream();
                mObjOutStream = new ObjectOutputStream(mOutStream);
                mObjInStream = new DynamicObjectInputStream(mInStream);

                Log.d(TAG, "Connection established with vm " + vmHelperIp);

                return true;
            } catch (Exception e) {
                Log.e(TAG, "Exception not caught properly - " + e);
                return false;
            } catch (Error e) {
                Log.e(TAG, "Error not caught properly - " + e);
                return false;
            }
        }

        private void pingOtherServer() {
            try {
                // Send a message to the Server Helper (other server)
                Log.d(TAG, "PING other server");
                mOutStream.write(eu.project.rapid.common.RapidMessages.PING);

                // Read and display the response message sent by server helper
                int response = mInStream.read();

                if (response == RapidMessages.PONG)
                    Log.d(TAG, "PONG from other server: " + vmHelperIp + ":" + config.getClonePort());
                else {
                    Log.d(TAG, "Bad Response to Ping - " + response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void closeConnection() {
            RapidUtils.closeQuietly(mObjOutStream);
            RapidUtils.closeQuietly(mObjInStream);
            RapidUtils.closeQuietly(mSocket);
        }
    }
}
