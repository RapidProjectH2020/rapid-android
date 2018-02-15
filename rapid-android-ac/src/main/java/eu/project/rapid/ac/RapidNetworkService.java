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
package eu.project.rapid.ac;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.project.rapid.ac.d2d.D2DMessage;
import eu.project.rapid.ac.d2d.D2DMessage.MsgType;
import eu.project.rapid.ac.d2d.PhoneSpecs;
import eu.project.rapid.ac.profilers.NetworkProfiler;
import eu.project.rapid.ac.utils.Constants;
import eu.project.rapid.ac.utils.Utils;
import eu.project.rapid.common.Clone;
import eu.project.rapid.common.Configuration;
import eu.project.rapid.common.RapidConstants;
import eu.project.rapid.common.RapidMessages;
import eu.project.rapid.common.RapidUtils;

import static android.util.Log.i;

/**
 * This thread will be started by clients that run the DFE so that these clients can get the HELLO
 * messages sent by the devices that act as D2D Acceleration Server.
 *
 * @author sokol
 */
public class RapidNetworkService extends IntentService {

    private static final String TAG = RapidNetworkService.class.getName();

    // Also used by the DFE, that's why not private
    static final int AC_RM_PORT = 23456;
    static final int AC_GET_VM = 1;
    static final int AC_GET_NETWORK_MEASUREMENTS = 2;
    static final int AC_QOS_PARAMS = 3;

    ScheduledThreadPoolExecutor setBroadcasterScheduledPool =
            (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private static final int FREQUENCY_BROADCAST_D2D_SET = 5 * 60 * 1013; // Every 5 minutes broadcast the set
    private TreeSet<PhoneSpecs> setD2dPhones = new TreeSet<>(); // Sorted by specs
    public static final String RAPID_D2D_SET_CHANGED = "eu.project.rapid.d2dSetUpdate"; // Broadcast intent
    public static final String RAPID_D2D_SET = "eu.project.rapid.d2dSet"; // Intent extra

    ScheduledThreadPoolExecutor netScheduledPool =
            (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);
    // Every 30 minutes measure rtt, ulRate, and dlRate
    private static final int FREQUENCY_NET_MEASUREMENT = 30 * 60 * 1000;

    ScheduledThreadPoolExecutor registrationScheduledPool =
            (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    // Every 2 minutes check if we have a VM. If not, try to re-register with the DS and SLAM.
    private static final int FREQUENCY_REGISTRATION = 2 * 60 * 1000;
    private static boolean registeringWithDs = false;
    private static boolean registeringWithSlam = false;

    private Configuration config;
    public static boolean usePrevVm = true;
    private Clone sClone;
    private long myId = -1;
    private String vmIp = "";
    private ArrayList<String> vmmIPs;
    private static final int vmNrVCPUs = 1; // FIXME: number of CPUs on the VM
    private static final int vmMemSize = 512; // FIXME
    private static final int vmNrGpuCores = 1200; // FIXME

    // Intent for sending broadcast messages
    public static final String RAPID_VM_CHANGED = "eu.project.rapid.vmChanged";
    public static final String RAPID_VM_IP = "eu.project.rapid.vmIP";
    public static final String RAPID_NETWORK_CHANGED = "eu.project.rapid.networkChanged";
    public static final String RAPID_NETWORK_RTT = "eu.project.rapid.rtt";
    public static final String RAPID_NETWORK_UL_RATE = "eu.project.rapid.ulRate";
    public static final String RAPID_NETWORK_DL_RATE = "eu.project.rapid.dlRate";
    private String jsonQosParams;

    private ExecutorService clientsThreadPool = Executors.newCachedThreadPool();

    public RapidNetworkService() {
        super(RapidNetworkService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Started the service");

        try (ServerSocket acRmServerSocket = new ServerSocket(AC_RM_PORT)) {
            Log.i(TAG, "************* Started the AC_RM listening server ****************");
            readConfigurationFile();
            String qosParams = readQosParams().replace(" ", "").replace("\n", "");
//            Log.v(TAG, "QoS params: " + qosParams);
            jsonQosParams = parseQosParams(qosParams);
            Log.v(TAG, "QoS params in JSON: " + jsonQosParams);

            // The prev id is useful to the DS so that it can release already allocated VMs.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            myId = prefs.getLong(Constants.MY_OLD_ID, -1);
            vmIp = prefs.getString(Constants.PREV_VM_IP, "");
            String vmmIp = prefs.getString(Constants.PREV_VMM_IP, "");

            new Thread(new D2DListener()).start();

            setBroadcasterScheduledPool.scheduleWithFixedDelay(new D2DSetBroadcaster(),
                    FREQUENCY_BROADCAST_D2D_SET, FREQUENCY_BROADCAST_D2D_SET, TimeUnit.MILLISECONDS);

            netScheduledPool.scheduleWithFixedDelay(new NetMeasurementRunnable(), 10,
                    FREQUENCY_NET_MEASUREMENT, TimeUnit.MILLISECONDS);

            registrationScheduledPool.scheduleWithFixedDelay(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (sClone == null) {
                                Log.v(TAG, "We do not have a VM, registering with the DS and SLAM...");
                                if (registeringWithDs || registeringWithSlam) {
                                    Log.v(TAG, "Registration already in progress...");
                                } else {
                                    registerWithDsAndSlam();
                                }
                            }
                        }
                    }, FREQUENCY_REGISTRATION, FREQUENCY_REGISTRATION, TimeUnit.MILLISECONDS
            );

            registerWithDsAndSlam();
            while (true) {
                try {
                    Socket client = acRmServerSocket.accept();
                    clientsThreadPool.execute(new ClientHandler(client));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Couldn't start the listening server, " +
                    "maybe another app has already started this service: " + e);
        } finally {
            if (clientsThreadPool != null) {
                clientsThreadPool.shutdown();
                Log.v(TAG, "The clientThreadPool is now shut down.");
            }

            if (netScheduledPool != null) {
                netScheduledPool.shutdown();
                Log.v(TAG, "The netScheduledPool is now shut down.");
            }

            if (registrationScheduledPool != null) {
                registrationScheduledPool.shutdown();
                Log.v(TAG, "The registrationScheduledPool is now shut down.");
            }
        }
    }

    private void readConfigurationFile() {
        try {
            // Read the config file to read the IP and port of Manager
            config = new Configuration(Constants.PHONE_CONFIG_FILE);
            config.parseConfigFile();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Config file not found: " + Constants.PHONE_CONFIG_FILE);
            config = new Configuration();
        }
    }

    /**
     * <p>Read the xml file with the QoS parameters, which has been created by the RAPID Compiler.</p>
     * <p>The format of the file is this one:</p>
     *
     * <pre>
     *     {@code
     * <application>
            <name>TODO</name>
            <class>
                <name>/Users/sokol/Desktop/test/demo/JniTest.java</name>
                <method>
                    <name>localjniCaller</name>
                    <Remote>
                        <computeIntensive>true</computeIntensive>
                    </Remote>
                    <QoS>
                        <term>cpu</term>
                        <operator>ge</operator>
                        <threshold>1200</threshold>
                        <term>ram</term>
                        <operator>ge</operator>
                        <threshold>500</threshold>
                    </QoS>
                </method>
            </class>
            ...
     *
     *      }
     * </pre>
     *
     * @return The content of the xml file as a string.
     */
    private String readQosParams () {
        String qosParams = "";
        try {
            qosParams = Utils.readAssetFileAsString(getApplicationContext(), Constants.QOS_FILENAME);
        } catch (IOException e) {
            Log.w(TAG, "Could not find QoS file - " + e);
        }

        return qosParams;
    }

    /**
     *
     * @param qosParams The string containing the xml QoS. We assume the QoS parameters are correct
     *                  (meaning that we do not perform error checking here, in terms of:
     *                  naming of QoS parameters, formatting of the QoS parameters, etc.).
     * @return The parsed string, converted then in json format:
     * {"QoS":[{"operator":"CPU_UTIL", "term":"LT", "threshold":60}, {"operator":"ram_util", "term":"lt", "threshold":1024}]}
     */
    private String parseQosParams(String qosParams) {
        StringBuilder jsonQosParams = new StringBuilder();
        jsonQosParams.append("{\"QoS\":[");
        List<String> terms = new LinkedList<>();
        List<String> operators = new LinkedList<>();
        List<String> thresholds = new LinkedList<>();

        // Remove all spaces and new lines
        qosParams = readQosParams().replace(" ", "").replace("\n", "");

        // Search for substrings starting with <QoS> and ending with </QoS>, excluding these tags
        Pattern qosPattern = Pattern.compile("(<QoS>(.*?)</QoS>)");
        Matcher qosMatcher = qosPattern.matcher(qosParams);

        Pattern termPattern = Pattern.compile("(<term>(.*?)</term>)");
        Pattern operatorPattern = Pattern.compile("(<operator>(.*?)</operator>)");
        Pattern thresholdPattern = Pattern.compile("(<threshold>(.*?)</threshold>)");

        while (qosMatcher.find()) {
            // Log.i(TAG, qosMatcher.group(2));
            // <term>cpu</term><operator>ge</operator><threshold>1500</threshold><term>ram</term><operator>ge</operator><threshold>1000</threshold>
            String qosString = qosMatcher.group(2);
            Matcher termMatcher = termPattern.matcher(qosString);
            Matcher operatorMatcher = operatorPattern.matcher(qosString);
            Matcher thresholdMatcher = thresholdPattern.matcher(qosString);

            while (termMatcher.find()) {
                terms.add(termMatcher.group(2));
            }

            while (operatorMatcher.find()) {
                operators.add(operatorMatcher.group(2));
            }

            while (thresholdMatcher.find()) {
                thresholds.add(thresholdMatcher.group(2));
            }
        }

        if (terms.size() != operators.size() || terms.size() != thresholds.size()) {
            Log.w(TAG, "QoS params not correctly formatted: number of terms, operators, and thresholds differ!");
        } else {
            for (int i = 0; i < terms.size(); i++) {
                jsonQosParams.append("{");
                jsonQosParams.append("\"term\":\"").append(terms.get(i)).append("\", ");
                jsonQosParams.append("\"operator\":\"").append(operators.get(i)).append("\", ");
                jsonQosParams.append("\"threshold\":");
                String th = thresholds.get(i);
                if (RapidUtils.isNumeric(th)) {
                    jsonQosParams.append(thresholds.get(i));
                } else {
                    jsonQosParams.append("\"").append(thresholds.get(i)).append("\"");
                }
                jsonQosParams.append("}");
                if (i < terms.size() - 1) {
                    jsonQosParams.append(", ");
                }
            }
        }

        jsonQosParams.append("]}");
        return jsonQosParams.toString();
    }

    private class D2DListener implements Runnable {
        private final String TAG = D2DListener.class.getName();

        @Override
        public void run() {

            try {
                i(TAG, "Thread started");
                broadcastD2dDevices();

                WifiManager.MulticastLock lock;
                WifiManager wifi = (WifiManager) getApplicationContext().
                        getSystemService(Context.WIFI_SERVICE);
                i(TAG, "Trying to acquire multicast lock...");
                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    if (wifi != null) {
                        lock = wifi.createMulticastLock("WiFi_Lock");
                        lock.setReferenceCounted(true);
                        lock.acquire();
                        i(TAG, "WiFi lock acquired (for multicast)!");
                    }
                }

                MulticastSocket receiveSocket = new MulticastSocket(Constants.D2D_BROADCAST_PORT);
                receiveSocket.setBroadcast(true);
                i(TAG, "Started listening on multicast socket.");

                try {
                    // This will be interrupted when the OS kills the service
                    while (true) {
                        i(TAG, "Waiting for broadcasted data...");
                        byte[] data = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(data, data.length);
                        receiveSocket.receive(packet);
                        Log.d(TAG, "Received a new broadcast packet from: " + packet.getAddress());
                        processPacket(packet);
                    }
                } catch (IOException e) {
                    Log.d(TAG, "The socket was closed.");
                }

                i(TAG, "Stopped receiving data!");
            } catch (IOException e) {
                // We expect this to happen when more than one DFE on the same phone will try to create
                // this service and the port will be busy. This way only one service will be listening for D2D
                // messages. This service will be responsible for writing the received messages on a file so
                // that the DFEs of all applications could read them.
                Log.d(TAG,
                        "Could not create D2D multicast socket, maybe the service is already started by another DFE: "
                                + e);
                // e.printStackTrace();
            }
        }
    }


    /**
     * Process the packet received by another device in a D2D scenario. Create a D2Dmessage and if
     * this is a HELLO message then store the specifics of the other device into the Map. If a new
     * device is added to the map and more than 5 minutes have passed since the last time we saved the
     * devices on the file, then save the set in the filesystem so that other DFEs can read it.
     *
     * @param packet The packet received by another device in a D2D scenario
     */
    private void processPacket(DatagramPacket packet) {
        try {
            D2DMessage msg = new D2DMessage(packet.getData());
            Log.d(TAG, "Received: <== " + msg);
            if (msg.getMsgType() == MsgType.HELLO) {
                PhoneSpecs otherPhone = msg.getPhoneSpecs();
                if (setD2dPhones.contains(otherPhone)) {
                    setD2dPhones.remove(otherPhone);
                }
                otherPhone.setTimestamp(System.currentTimeMillis());
                otherPhone.setIp(packet.getAddress().getHostAddress());
                setD2dPhones.add(otherPhone);
                // FIXME writing the set here is too heavy but I want this just for the demo.
                // Later fix this with a smarter alternative.
                broadcastD2dDevices();
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "Error while processing the packet: " + e);
        }
    }

    private class D2DSetBroadcaster implements Runnable {
        @Override
        public void run() {
            // Broadcast the set to other DFEs.
            // First clean the set from devices that have not been pinging recently.
            Iterator<PhoneSpecs> it = setD2dPhones.iterator();
            while (it.hasNext()) {
                // If the last time we have seen this device is 5 pings before, then remove it.
                if ((System.currentTimeMillis() - it.next().getTimestamp()) > 5
                        * Constants.D2D_BROADCAST_INTERVAL) {
                    it.remove();
                }
            }
            broadcastD2dDevices();
        }
    }

    private void broadcastD2dDevices() {
        i(TAG, "Broadcasting (internally) set of d2d devices, if not empty (size: " +
                (setD2dPhones != null ? setD2dPhones.size() : "null") + ")");
        if (setD2dPhones != null && setD2dPhones.size() > 0) {
            Intent i = new Intent(RAPID_D2D_SET_CHANGED);
            i.putExtra(RAPID_D2D_SET, setD2dPhones);
            sendBroadcast(i);
        }
    }

    private class NetMeasurementRunnable implements Runnable {
        @Override
        public void run() {
            int maxWaitTime = 5 * 60 * 1000; // 5 min
            int waitingSoFar = 0;

            while (sClone == null && waitingSoFar < maxWaitTime) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                waitingSoFar += 1000;
            }

            if (sClone != null) {
                NetworkProfiler.measureRtt(sClone.getIp(), config.getClonePortBandwidthTest());
                NetworkProfiler.measureUlRate(sClone.getIp(), config.getClonePortBandwidthTest());
                NetworkProfiler.measureDlRate(sClone.getIp(), config.getClonePortBandwidthTest());

                Intent intent = new Intent(RapidNetworkService.RAPID_NETWORK_CHANGED);
                intent.putExtra(RapidNetworkService.RAPID_NETWORK_RTT, NetworkProfiler.rtt);
                intent.putExtra(RapidNetworkService.RAPID_NETWORK_DL_RATE, NetworkProfiler.lastDlRate.getBw());
                intent.putExtra(RapidNetworkService.RAPID_NETWORK_UL_RATE, NetworkProfiler.lastUlRate.getBw());
                sendBroadcast(intent);
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;

        ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (InputStream is = clientSocket.getInputStream();
                 OutputStream os = clientSocket.getOutputStream();
                 ObjectInputStream ois = new ObjectInputStream(is);
                 ObjectOutputStream oos = new ObjectOutputStream(os)) {

                int command = is.read();
                switch (command) {
                    case AC_GET_VM:
                        oos.writeObject(sClone);
                        oos.flush();
                        break;

                    case AC_GET_NETWORK_MEASUREMENTS:
                        oos.writeInt(NetworkProfiler.lastUlRate.getBw());
                        oos.writeInt(NetworkProfiler.lastDlRate.getBw());
                        oos.writeInt(NetworkProfiler.rtt);
                        oos.flush();
                        break;

                    case AC_QOS_PARAMS:
                        break;

                    default:
                        Log.w(TAG, "Did not recognize command: " + command);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized boolean registerWithDsAndSlam() {
        i(TAG, "Registering...");
        boolean registeredWithSlam = false;

        if (registerWithDs()) {
            // register with SLAM. Send the VMM where we want to start the VM
            int vmmIndex = 0;
            if (vmmIPs != null) {
                do {
                    registeredWithSlam = registerWithSlam(vmmIPs.get(vmmIndex));
                    vmmIndex++;
                } while (!registeredWithSlam && vmmIndex < vmmIPs.size());
            }
        }
        return registeredWithSlam;
    }

    /**
     * Read the config file to get the IP and port of the DS. The DS will return a list of available
     * SLAMs, choose the best one from the list and connect to it to ask for a VM.
     */
    @SuppressWarnings("unchecked")
    private boolean registerWithDs() {

        Log.d(TAG, "Starting as phone with ID: " + myId);

        int maxNrTimesToTry = 3;
        int nrTimesTried = 0;
        Socket dsSocket = null;
        boolean connectedWithDs = false;
        do {
            registeringWithDs = true;
            i(TAG, "Registering with DS " + config.getDSIp() + ":" + config.getDSPort());
            try {
                dsSocket = new Socket();
                dsSocket.connect(new InetSocketAddress(config.getDSIp(), config.getDSPort()), 3000);
                Log.d(TAG, "Connected with DS");
                connectedWithDs = true;
            } catch (Exception e) {
                Log.e(TAG, "Could not connect with the DS: " + e);
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }
        } while (!connectedWithDs && ++nrTimesTried < maxNrTimesToTry);

        if (connectedWithDs) {
            try (ObjectOutputStream dsOut = new ObjectOutputStream(dsSocket.getOutputStream());
                 ObjectInputStream dsIn = new ObjectInputStream(dsSocket.getInputStream())) {

                // Send the name and id to the DS
                if (usePrevVm && myId != -1) {
                    i(TAG, "AC_REGISTER_PREV_DS");
                    // Send message format: command (java byte), userId (java long), qosFlag (java int)
                    dsOut.writeByte(RapidMessages.AC_REGISTER_PREV_DS);
                    dsOut.writeLong(myId); // send my user ID so that my previous VM can be released
                } else {
                    i(TAG, "AC_REGISTER_NEW_DS");
                    dsOut.writeByte(RapidMessages.AC_REGISTER_NEW_DS);

                    dsOut.writeLong(myId); // send my user ID so that my previous VM can be released
                    // FIXME: should not use hard-coded values here.
                    dsOut.writeInt(vmNrVCPUs); // send vcpuNum as int
                    dsOut.writeInt(vmMemSize); // send memSize as int
                    dsOut.writeInt(vmNrGpuCores); // send gpuCores as int
                }

                dsOut.flush();
                byte status = dsIn.readByte();
                i(TAG, "Return Status: " + (status == RapidMessages.OK ? "OK" : "ERROR"));
                if (status == RapidMessages.OK) {
                    myId = dsIn.readLong();
                    i(TAG, "userId is: " + myId);

                    // Read the list of VMMs, which will be sorted based on free resources
                    vmmIPs = (ArrayList<String>) dsIn.readObject();

                    // Read the SLAM IP and port
                    String slamIp = dsIn.readUTF();
                    int slamPort = dsIn.readInt();
                    config.setSlamIp(slamIp);
                    config.setSlamPort(slamPort);
                    i(TAG, "SLAM address is: " + slamIp + ":" + slamPort);

                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while connecting with the DS: " + e);
                e.printStackTrace();
            }
        }

        registeringWithDs = false;
        return false;
    }

    /**
     * Register with the SLAM.
     */
    private boolean registerWithSlam(String vmmIp) {

        int maxNrTimesToTry = 3;
        int nrTimesTried = 0;
        Socket slamSocket = null;
        boolean connectedWithSlam = false;

        do {
            i(TAG, "Registering with SLAM " + config.getSlamIp() + ":" + config.getSlamPort());
            registeringWithSlam = true;
            try {
                slamSocket = new Socket();
                slamSocket.connect(new InetSocketAddress(config.getSlamIp(), config.getSlamPort()), 5000);
                Log.d(TAG, "Connected with SLAM");
                connectedWithSlam = true;
            } catch (Exception e) {
                Log.e(TAG, "Could not connect with the SLAM: " + e);
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }
        } while (!connectedWithSlam && ++nrTimesTried < maxNrTimesToTry);

        if (connectedWithSlam) {
            try (ObjectOutputStream oos = new ObjectOutputStream(slamSocket.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(slamSocket.getInputStream())) {

                // Send the ID to the SLAM
                oos.writeByte(RapidMessages.AC_REGISTER_SLAM);
                oos.writeLong(myId);
                oos.writeInt(RapidConstants.OS.ANDROID.ordinal());

                // Send the vmmId and vmmPort to the SLAM so it can start the VM
                oos.writeUTF(vmmIp);
                oos.writeInt(config.getVmmPort());

                // FIXME: should not use hard-coded values here.
                oos.writeInt(vmNrVCPUs); // send vcpuNum as int
                oos.writeInt(vmMemSize); // send memSize as int
                oos.writeInt(vmNrGpuCores); // send gpuCores as int
                oos.writeUTF(jsonQosParams);

                oos.flush();

                Log.i(TAG, "Waiting for SLAM to reply...");
                int slamResponse = ois.readByte();
                if (slamResponse == RapidMessages.OK) {
                    i(TAG, "SLAM OK, getting the VM details");
                    vmIp = ois.readUTF();

                    sClone = new Clone("", vmIp);
                    sClone.setId((int) myId);

                    i(TAG, "Saving my ID and the vmIp: " + myId + ", " + vmIp);
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = prefs.edit();

                    editor.putLong(Constants.MY_OLD_ID, myId);
                    editor.putString(Constants.PREV_VM_IP, vmIp);

                    i(TAG, "Saving the VMM IP: " + vmmIp);
                    editor.putString(Constants.PREV_VMM_IP, vmmIp);
                    editor.apply();

                    i(TAG, "Broadcasting the details of the VM to all rapid apps");
                    Intent intent = new Intent(RapidNetworkService.RAPID_VM_CHANGED);
                    intent.putExtra(RapidNetworkService.RAPID_VM_IP, sClone);
                    sendBroadcast(intent);

                    return true;

                } else if (slamResponse == RapidMessages.ERROR) {
                    Log.e(TAG, "SLAM registration replied with ERROR, VM will be null");
                } else {
                    Log.e(TAG, "SLAM registration replied with uknown message " + slamResponse
                            + ", VM will be null");
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException while talking to the SLAM: " + e);
            }
        }

        registeringWithSlam = false;
        return false;
    }
}
