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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import eu.project.rapid.ac.d2d.D2DMessage;
import eu.project.rapid.ac.utils.Constants;
import eu.project.rapid.ac.utils.Utils;
import eu.project.rapid.common.Configuration;
import eu.project.rapid.common.RapidConstants;
import eu.project.rapid.common.RapidMessages;
import eu.project.rapid.common.RapidMessages.AnimationMsg;
import eu.project.rapid.common.RapidUtils;

/**
 * Execution server which waits for incoming connections and starts a separate thread for each of
 * them, leaving the AppHandler to actually deal with the clients
 */
public class AccelerationServer extends Service {

    private Context context;

    private static final String TAG = "AccelerationServer";
    private Configuration config;

    static long userId = -1; // The userId will be given by the VMM
    static long vmId = -1; // The vmId will be assigned by the DS
    static String vmIp = ""; // The vmIp should be extracted by us

    private Handler mBroadcastHandler;
    private Runnable mBroadcastRunnable;
    private ExecutorService threadPool = Executors.newFixedThreadPool(1000);
    static String arch = System.getProperty("os.arch");

    // Using the BC
//    static {
//        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
//    }

    /**
     * Called when the service is first created.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Server created, running on arch: " + arch);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Server destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create server socket
        Log.d(TAG, "Start server socket");
        context = this.getApplicationContext();
        if (context == null) {
            Log.e(TAG, "Context is null!!!");
            stopSelf();
        }

        // Create a special file on the clone that methods can use to check
        // if are being executed on the clone or on the phone.
        // This can be of help to advanced developers.
        if (createOrGetRapidFolder()) {
            createOffloadedFile();
            copyCryptoKeysFromAssets();
        } else {
            Log.e(TAG, "Error while creating RAPID folder: " + Constants.RAPID_FOLDER);
        }

        // Delete the file containing the cloneHelperId assigned to this clone
        // (if such file does not exist do nothing).
        Utils.deleteCloneHelperId();

        try {
            config = new Configuration(Constants.CLONE_CONFIG_FILE);
            config.parseConfigFile();
        } catch (FileNotFoundException e1) {
            Log.e(TAG, "Configuration file not found on the clone: " + Constants.CLONE_CONFIG_FILE);
            Log.e(TAG, "Continuing with default values.");
            config = new Configuration();
        }

        try {
            Log.i(TAG, "KeyStore default type: " + KeyStore.getDefaultType());
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(context.getAssets().open(Constants.SSL_KEYSTORE),
                    Constants.SSL_DEFAULT_PASSW.toCharArray());
            KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, Constants.SSL_DEFAULT_PASSW.toCharArray());

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(Constants.SSL_CERT_ALIAS,
                    Constants.SSL_DEFAULT_PASSW.toCharArray());
            Certificate cert = keyStore.getCertificate(Constants.SSL_CERT_ALIAS);
            PublicKey publicKey = cert.getPublicKey();

            config.setCryptoInitialized(true);
            config.setKmf(kmf);
            config.setPublicKey(publicKey);
            config.setPrivateKey(privateKey);

            Log.i(TAG, "Certificate: " + cert.toString());
            Log.i(TAG, "PrivateKey algorithm: " + privateKey.getAlgorithm());
            Log.i(TAG, "PublicKey algorithm: " + publicKey.getAlgorithm());

        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException
                | UnrecoverableKeyException e) {
            Log.e(TAG, "Crypto not initialized: " + e);
        }

        // Connect to the manager to register and get the configuration details
        Thread t = new Thread(new RegistrationManager());
        t.start();

        return START_STICKY;
    }

    private boolean createOrGetRapidFolder() {
        File f = new File(Constants.RAPID_FOLDER);
        return f.exists() || f.mkdirs();
    }

    /**
     * Creates a sentinel file on the clone in order to let the method know it is being executed on
     * the clone.
     */
    private void createOffloadedFile() {
        try {
            if (new File(Constants.FILE_OFFLOADED).createNewFile()) {
                Log.i(TAG, "The offloaded file didn't exist and was created: " + Constants.FILE_OFFLOADED);
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not create offloaded file: " + e);
        }
    }

    private void copyCryptoKeysFromAssets() {
        Log.v(TAG, "Copying the keystore from assets to the rapid folder...");
        try (InputStream is = this.getAssets().open("keystore.bks");
             OutputStream fos = new FileOutputStream(Constants.SSL_KEYSTORE)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            Log.v(TAG, "Finished copying the keystore!");
        } catch (IOException e) {
            Log.v(TAG, "Exception while copying the keystore: " + e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Read the config file to get the IP and port for the Manager.
     */
    private class RegistrationManager implements Runnable {

        @Override
        public void run() {

            // Before proceeding wait until the network interface is up and correctly configured
            vmIp = waitForNetworkToBeUp();
            Log.i(TAG, "My IP: " + vmIp);

//            if (vmIp.startsWith("10.0")) {
            Log.i(TAG, "Build.hardware=" + Build.HARDWARE);
            if (isEmulator()) {
                Log.i(TAG, "Running on VM on the cloud under VPN, trying to connect to VMM and DS...");
                if (registerWithVmmAndDs()) {
                    startClientListeners();
                } else {
                    Log.e(TAG, "Could not register with the VMM and DS, not starting the listeners.");

                    Log.e(TAG, "######################## FIXME: Starting the listeners for testing anyway ########################");
                    startClientListeners();
                    Log.e(TAG, "######################## END FIXME ########################");
                }
            } else {
                Log.i(TAG, "Running on a phone for D2D offloading, starting the listeners");
                startClientListeners();

                // Start the D2D handler that broadcasts ping messages with a certain frequency
                startD2DThread();

                // Check if the primary animation server is reachable
                boolean primaryAnimationServerReachable = RapidUtils.isPrimaryAnimationServerRunning(
                        config.getAnimationServerIp(), config.getAnimationServerPort());
                Log.i(TAG, "Primary animation server reachable: " + primaryAnimationServerReachable);
                if (!primaryAnimationServerReachable) {
                    config.setAnimationServerIp(RapidConstants.DEFAULT_SECONDARY_ANIMATION_SERVER_IP);
                    config.setAnimationServerPort(RapidConstants.DEFAULT_SECONDARY_ANIMATION_SERVER_PORT);
                }
            }
        }

        private String waitForNetworkToBeUp() {

            InetAddress vmIpAddress;
            do {
                vmIpAddress = Utils.getIpAddress();
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            while (vmIpAddress == null || !RapidUtils.validateIpAddress(vmIpAddress.getHostAddress()));
            Log.i(TAG, "I have an IP: " + vmIpAddress.getHostAddress());

            // FIXME: remove the hard-coded 10.0.*
            if (vmIpAddress.getHostAddress().startsWith("10.0")) {
//            if (isEmulator()) {
                // This is a VM running on the cloud
                boolean hostMachineReachable = false;
                do {
                    try {
                        // The VMM runs on the host machine, so checking if we can ping the vmmIP in
                        // reality we are checking if we can ping the host machine.
                        // We should definitely be able to do that, otherwise this clone is useless if not
                        // connected to the network.

                        InetAddress hostMachineAddress = InetAddress.getByName(config.getVmmIp());
                        try {
                            Log.i(TAG,
                                    "Trying to ping the host machine " + hostMachineAddress.getHostAddress());
                            hostMachineReachable = hostMachineAddress.isReachable(5000);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        } catch (IOException e) {
                            Log.w(TAG, "Error while trying to ping the host machine: " + e);
                        }
                    } catch (UnknownHostException e1) {
                        Log.e(TAG, "Error while getting hostname: " + e1);
                    }
                } while (!hostMachineReachable);
                Log.i(TAG, "Host machine replied to ping. Network interface is up and running.");
            }

            return vmIpAddress.getHostAddress();
        }

        private boolean registerWithVmmAndDs() {

            Socket vmmSocket = null;
            ObjectOutputStream vmmOut = null;
            ObjectInputStream vmmIn = null;

            try {
                Log.d(TAG, "Connecting to VMM: " + config.getVmmIp() + ":" + config.getVmmPort());

                vmmSocket = new Socket();
                vmmSocket.connect(new InetSocketAddress(config.getVmmIp(), config.getVmmPort()), 3 * 1000);
                vmmOut = new ObjectOutputStream(vmmSocket.getOutputStream());
                vmmIn = new ObjectInputStream(vmmSocket.getInputStream());

                Log.d(TAG, "Sending myIp: " + vmIp);
                vmmOut.writeByte(RapidMessages.AS_RM_REGISTER_VMM);
                vmmOut.writeUTF(vmIp);
                vmmOut.flush();

                // Receive message format: status (java byte), userId (java long)
                byte status = vmmIn.readByte();
                Log.i(TAG, "VMM return Status: " + (status == RapidMessages.OK ? "OK" : "ERROR"));
                if (status == RapidMessages.OK) {
                    userId = vmmIn.readLong();
                    Log.i(TAG, "userId is: " + userId);

                    if (registerWithDs()) {
                        // Notify the VMM that the registration with the DS was correct
                        Log.i(TAG, "Correctly registered with the DS");
                        vmmOut.writeByte(RapidMessages.OK);
                        vmmOut.flush();
                        return true;
                    } else {
                        // Notify the VMM that the registration with the DS was correct
                        Log.i(TAG, "Registration with the DS failed");
                        vmmOut.writeByte(RapidMessages.ERROR);
                        vmmOut.flush();
                        return false;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not connect with the VMM: " + e);
            } finally {
                Log.d(TAG, "Done talking with the VMM");
                // Close the connection with the VMM
                RapidUtils.closeQuietly(vmmOut);
                RapidUtils.closeQuietly(vmmIn);
                RapidUtils.closeQuietly(vmmSocket);
            }
            return false;
        }

        private boolean registerWithDs() {
            Log.i(TAG, "Registering with the DS...");

            Socket dsSocket = null;
            ObjectOutputStream dsOut = null;
            ObjectInputStream dsIn = null;

            try {
                dsSocket = new Socket(config.getDSIp(), config.getDSPort());
                dsOut = new ObjectOutputStream(dsSocket.getOutputStream());
                // dsOut.flush();
                dsIn = new ObjectInputStream(dsSocket.getInputStream());

                // Send message format: command (java byte), userId (java long)
                dsOut.writeByte(RapidMessages.AS_RM_REGISTER_DS);
                dsOut.writeLong(userId); // userId
                dsOut.flush();

                // Receive message format: status (java byte), vmId (java long)
                byte status = dsIn.readByte();
                System.out.println("Return Status: " + (status == RapidMessages.OK ? "OK" : "ERROR"));
                if (status == RapidMessages.OK) {
                    vmId = dsIn.readLong();
                    Log.i(TAG, "Received vmId: " + vmId);
                    return true;
                }

                // Maybe we can add this functionality, the DS to send information about other components.
                // config.setAnimationServerIp(dsIn.readUTF());
                // config.setAnimationServerPort(dsIn.readInt());

            } catch (IOException e) {
                Log.e(TAG, "Registering with the DS failed: " + e);
            } finally {
                RapidUtils.closeQuietly(dsOut);
                RapidUtils.closeQuietly(dsIn);
                RapidUtils.closeQuietly(dsSocket);
            }

            return false;
        }
    }

    private void startClientListeners() {

        Log.d(TAG, "Starting NetworkProfilerServer on port " + config.getClonePortBandwidthTest());
        new Thread(new NetworkProfilerServer(config)).start();

        Log.d(TAG, "Starting ClientListenerClear on port " + config.getClonePort());
        new Thread(new ClientListenerClear(context, config)).start();

        if (config.isCryptoInitialized()) {
            Log.d(TAG, "Starting ClientListenerSSL on port " + config.getSslClonePort());
            new Thread(new ClientListenerSSL(context, config)).start();
        } else {
            Log.w(TAG,
                    "Cannot start the CloneSSLThread since the cryptographic initialization was not succesful");
        }
    }


    /**
     * The thread that listens for new clients (phones or other clones) to connect in clear.
     */
    private class ClientListenerClear implements Runnable {

        private static final String TAG = "ClientListenerClear";

        private Context context;
        private Configuration config;

        public ClientListenerClear(Context context, eu.project.rapid.common.Configuration config) {
            this.context = context;
            this.config = config;
        }

        @Override
        public void run() {

            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(config.getClonePort());
                Log.i(TAG, "ClientListenerClear started on port " + config.getClonePort());
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    Log.i(TAG, "New client connected in clear");
                    threadPool.execute(new AppHandler(clientSocket, context, config));
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error while closing server socket: " + e);
                    }
                }
            }
        }
    }

    /**
     * The thread that listens for new clients (phones or other clones) to connect using SSL.
     */
    public class ClientListenerSSL implements Runnable {

        private static final String TAG = "ClientListenerSSL";

        private Context context;
        private eu.project.rapid.common.Configuration config;

        public ClientListenerSSL(Context context, Configuration config) {
            this.context = context;
            this.config = config;
        }

        @Override
        public void run() {

            Log.i(TAG, "ClientListenerSSL started");

            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(config.getKmf().getKeyManagers(), null, null);

                SSLServerSocketFactory factory =
                        (SSLServerSocketFactory) sslContext.getServerSocketFactory();
                Log.i(TAG, "factory created");

                SSLServerSocket serverSocket =
                        (SSLServerSocket) factory.createServerSocket(config.getSslClonePort());
                Log.i(TAG, "server socket created");

                // If we want also the client to authenticate himself
                // serverSocket.setNeedClientAuth(true); // default is false

                while (true) {
                    // Log.i(TAG, "Saved session IDs: ");
                    // Enumeration<byte[]> sessionIDs = sslContext.getServerSessionContext().getIds();
                    // while(sessionIDs.hasMoreElements()) {
                    // Log.i(TAG, "ID: " + RapidUtils.bytesToHex(sessionIDs.nextElement()));
                    // }
                    // Log.i(TAG, "");
                    //
                    Socket clientSocket = serverSocket.accept();
                    Log.i(TAG, "New client connected using SSL");
                    threadPool.execute(new AppHandler(clientSocket, context, config));
                }

            } catch (IOException | NoSuchAlgorithmException | KeyManagementException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * FIXME This thread should run only on the mobile devices, so it should not be started
     * automatically here but the user should have to press a button to start it.
     */
    private void startD2DThread() {
        Looper.prepare();
        if (mBroadcastHandler == null) {
            mBroadcastHandler = new Handler();
        }
        if (mBroadcastRunnable == null) {
            mBroadcastRunnable = new D2DBroadcastThread();
        }
        mBroadcastHandler.postDelayed(mBroadcastRunnable, Constants.D2D_BROADCAST_INTERVAL);
        Looper.loop();
    }

    private class D2DBroadcastThread implements Runnable {
        public void run() {
            Log.i(TAG, "Running the broadcast message runnable");
            if (context == null) {
                Log.e(TAG, "Context is null!!!");
            }
            broadcastMessage(new D2DMessage(context, D2DMessage.MsgType.HELLO));
            mBroadcastHandler.postDelayed(mBroadcastRunnable, Constants.D2D_BROADCAST_INTERVAL);
        }

        private void broadcastMessage(final D2DMessage msg) {
            new Thread() {
                public void run() {
                    DatagramPacket packet;
                    MulticastSocket socket = null;
                    try {
                        byte[] data = Utils.objectToByteArray(msg);

                        socket = new MulticastSocket(Constants.D2D_BROADCAST_PORT);
                        socket.setBroadcast(true);

                        InetAddress myIpAddress = Utils.getIpAddress();
                        if (myIpAddress != null) {
                            Log.i(TAG, "My IP address: " + myIpAddress.getHostAddress());

                            InetAddress broadcastAddress = Utils.getBroadcast(myIpAddress);
                            Log.i(TAG, "Broadcast IP address: " + broadcastAddress);
                            try {
                                packet = new DatagramPacket(data, data.length, broadcastAddress,
                                        Constants.D2D_BROADCAST_PORT);
                                socket.send(packet);
                                Log.i(TAG, "==>> Broadcast message sent to " + broadcastAddress);
                                Log.i(TAG, "==>> CMD: " + msg);
                                RapidUtils.sendAnimationMsg(config, AnimationMsg.AS_BROADCASTING_D2D);

                            } catch (NullPointerException | IOException e) {
                                Log.e(TAG, "Exception while sending data: " + e);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while sending data: " + e);
                    } finally {
                        if (socket != null) {
                            socket.close();
                        }
                    }
                }
            }.start();
        }
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT)
                || Build.HARDWARE.contains("android_x86");
    }
}
