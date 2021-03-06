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
package eu.project.rapid.ac.profilers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import eu.project.rapid.common.Configuration;
import eu.project.rapid.common.RapidMessages;
import eu.project.rapid.common.RapidUtils;

/**
 * Network information profiler
 */
// @TargetApi(8)
public class NetworkProfiler {
    private static final String TAG = "NetworkProfiler";

    private static final int rttPings = 5;
    public static final int rttInfinite = 100000000;
    public static int rtt = rttInfinite;

    // Keep the upload/download data rate history between the phone and the clone
    // Data rate in b/s
    private static final int bwWindowMaxLength = 20;
    private static List<NetworkBWRecord> ulRateHistory = new LinkedList<>();
    private static List<NetworkBWRecord> dlRateHistory = new LinkedList<>();
    public static NetworkBWRecord lastUlRate = new NetworkBWRecord();
    public static NetworkBWRecord lastDlRate = new NetworkBWRecord();

    public static String currentNetworkTypeName;
    public static String currentNetworkSubtypeName;
    private static byte[] buffer;
    private static final int BUFFER_SIZE = 10 * 1024;

    private Context context;
    private static Configuration config;
    private static NetworkInfo netInfo;
    private static TelephonyManager telephonyManager;
    private static ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private static BroadcastReceiver networkStateReceiver;

    private boolean stopEstimatingEnergy;
    private ArrayList<Long> wifiTxPackets;
    private ArrayList<Long> wifiRxPackets;
    private ArrayList<Long> wifiTxBytes; // uplink data rate
    private ArrayList<Byte> threeGActiveState;
    public static final byte THREEG_IN_IDLE_STATE = 0;
    public static final byte THREEG_IN_FACH_STATE = 1;
    public static final byte THREEG_IN_DCH_STATE = 2;

    // For measuring the nr of bytes sent and received
    private final static int uid = android.os.Process.myUid();
    private long duration;
    // Needed by Profiler
    long rxBytes;
    long txBytes;

    /**
     * Constructor used to create a network profiler instance during method execution
     */
    public NetworkProfiler() {
        stopEstimatingEnergy = false;
        wifiTxPackets = new ArrayList<>();
        wifiRxPackets = new ArrayList<>();
        wifiTxBytes = new ArrayList<>();
        threeGActiveState = new ArrayList<>();
    }

    /**
     * Constructor used to create the network profiler instance of the DFE
     *
     * @param context
     */
    public NetworkProfiler(Context context, Configuration config) {
        this();
        this.context = context;
        NetworkProfiler.config = config;
        buffer = new byte[BUFFER_SIZE];

        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            throw new NullPointerException("WiFi manager is null");
        }
    }

    private static void addNewUlRateEstimate(long bytes, long nanoTime) {

        Log.d(TAG, "Sent " + bytes + " bytes in " + nanoTime + "ns");
        int ulRate = (int) ((((double) 8 * bytes) / nanoTime) * 1000000000);
        Log.i(TAG, "Estimated upload bandwidth: " + ulRate + " b/s (" + ulRate / 1000 + " Kbps)");

        // Rule 1: if the number of bytes sent was bigger than 10KB and the ulRate is small then keep
        // it, otherwise throw it
        // Rule 2: if the number of bytes sent was bigger than 50KB then keep the calculated ulRate
        if (bytes < 10 * 1000) {
            return;
        } else if (bytes < 50 * 1000 && ulRate > 250 * 1000) {
            return;
        }

        if (ulRateHistory.size() >= bwWindowMaxLength) {
            ulRateHistory.remove(0);
        }

        lastUlRate = new NetworkBWRecord(ulRate, System.currentTimeMillis());
        ulRateHistory.add(lastUlRate);

        // uploadRateHandler.removeCallbacks(uploadRunnable);
        // uploadRateHandler.postDelayed(uploadRunnable, delayRefreshUlRate);
    }

    private static void addNewDlRateEstimate(long bytes, long nanoTime) {

        Log.d(TAG, "Received " + bytes + " bytes in " + nanoTime + "ns");
        int dlRate = (int) ((((double) 8 * bytes) / nanoTime) * 1000000000);
        Log.i(TAG, "Estimated download bandwidth: " + dlRate + " b/s (" + dlRate / 1000 + " Kbps)");

        // Rule 1: if the number of bytes sent was bigger than 10KB and the ulRate is small then keep
        // it, otherwise throw it
        // Rule 2: if the number of bytes sent was bigger than 50KB then keep the calculated ulRate
        if (bytes < 10 * 1000) {
            return;
        } else if (bytes < 50 * 1000 && dlRate > 250 * 1000) {
            return;
        }

        if (dlRateHistory.size() >= bwWindowMaxLength)
            dlRateHistory.remove(0);

        lastDlRate = new NetworkBWRecord(dlRate, System.currentTimeMillis());
        dlRateHistory.add(lastDlRate);

        // downloadRateHandler.removeCallbacks(downloadRunnable);
        // downloadRateHandler.postDelayed(downloadRunnable, delayRefreshDlRate);
    }

    /**
     * Doing a few pings on a given connection to measure how big the RTT is between the client and
     * the remote machine
     *
     * @param in
     * @param out
     * @return
     */
    private static int rttPing(InputStream in, OutputStream out) {
        Log.d(TAG, "Pinging");
        int tRtt = 0;
        int response;
        try {
            for (int i = 0; i < rttPings; i++) {
                Long start = System.nanoTime();
                Log.d(TAG, "Send Ping");
                out.write(eu.project.rapid.common.RapidMessages.PING);

                Log.d(TAG, "Read Response");
                response = in.read();
                if (response == RapidMessages.PONG)
                    tRtt = (int) (tRtt + (System.nanoTime() - start) / 2);
                else {
                    Log.d(TAG, "Bad Response to Ping - " + response);
                    tRtt = rttInfinite;
                }

            }
            rtt = tRtt / rttPings;
            Log.d(TAG, "Ping - " + rtt / 1000000 + "ms");

        } catch (Exception e) {
            Log.e(TAG, "Error while measuring RTT: " + e);
            rtt = rttInfinite;
        }
        return rtt;
    }

    /**
     * Start counting transmitted data at a certain point for the current process (RX/TX bytes from
     * /sys/class/net/proc/uid_stat)
     */
    void startTransmittedDataCounting() {

        rxBytes = getProcessRxBytes();
        txBytes = getProcessTxBytes();
        duration = System.nanoTime();

        if (telephonyManager != null) {
            if (currentNetworkTypeName.equals("WIFI")) {
                calculateWifiRxTxPackets();
            } else {
                calculate3GStates();
            }
        }
    }

    /**
     * Stop counting transmitted data and store it in the profiler object
     */
    void stopAndCollectTransmittedData() {

        synchronized (this) {
            stopEstimatingEnergy = true;
        }

        // Need this for energy estimation
        if (telephonyManager != null) {
            calculatePacketRate();
            calculateUplinkDataRate();
        }

        rxBytes = getProcessRxBytes() - rxBytes;
        txBytes = getProcessTxBytes() - txBytes;
        duration = System.nanoTime() - duration;

        addNewDlRateEstimate(rxBytes, duration);
        addNewUlRateEstimate(txBytes, duration);

        Log.d(TAG, "UID: " + uid + " RX bytes: " + rxBytes + " TX bytes: " + txBytes + " duration: "
                + duration + " ns");
    }

    /**
     * @return RX bytes
     */
    public static Long getProcessRxBytes() {
        return TrafficStats.getUidRxBytes(uid);
    }

    /**
     * @return TX bytes
     */
    public static Long getProcessTxBytes() {
        return TrafficStats.getUidTxBytes(uid);
    }

    /**
     * @return Number of packets transmitted
     */
    private static Long getProcessTxPackets() {
        return TrafficStats.getUidTxPackets(uid);
    }

    /**
     * @return Number of packets received
     */
    private static Long getProcessRxPackets() {
        return TrafficStats.getUidRxPackets(uid);
    }

    /**
     * Intent based network state tracking - helps to monitor changing conditions without the
     * overheads of polling and only updating when needed (i.e. when something actually has changes)
     */
    public void registerNetworkStateTrackers() {
        networkStateReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {

                netInfo = connectivityManager.getActiveNetworkInfo();
                if (netInfo == null) {
                    Log.d(TAG, "No Connectivity");
                    currentNetworkTypeName = "";
                    currentNetworkSubtypeName = "";
                } else {
                    Log.d(TAG, "Connected to network type " + netInfo.getTypeName() + " subtype "
                            + netInfo.getSubtypeName());
                    currentNetworkTypeName = netInfo.getTypeName();
                    currentNetworkSubtypeName = netInfo.getSubtypeName();
                }
            }
        };

        Log.d(TAG, "Register Connectivity State Tracker");
        IntentFilter networkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(networkStateReceiver, networkStateFilter);

        PhoneStateListener listener = new PhoneStateListener() {
            @Override
            public void onDataConnectionStateChanged(int state, int networkType) {
                if (state == TelephonyManager.DATA_CONNECTED) {
                    if (networkType == TelephonyManager.NETWORK_TYPE_EDGE)
                        Log.d(TAG, "Connected to EDGE network");
                    else if (networkType == TelephonyManager.NETWORK_TYPE_GPRS)
                        Log.d(TAG, "Connected to GPRS network");
                    else if (networkType == TelephonyManager.NETWORK_TYPE_UMTS)
                        Log.d(TAG, "Connected to UMTS network");
                    else
                        Log.d(TAG, "Connected to other network - " + networkType);
                } else if (state == TelephonyManager.DATA_DISCONNECTED) {
                    Log.d(TAG, "Data connection lost");
                } else if (state == TelephonyManager.DATA_SUSPENDED) {
                    Log.d(TAG, "Data connection suspended");
                }
            }
        };

        Log.d(TAG, "Register Telephony Data Connection State Tracker");
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
    }

    public static boolean isNetConnectionAvailable() {
        return !currentNetworkTypeName.equals("");
    }

    /**
     * Get the number of packets Tx and Rx every second and update the arrays.<br>
     */
    private void calculateWifiRxTxPackets() {
        Thread t = new Thread() {
            public void run() {

                while (!stopEstimatingEnergy) {

                    wifiRxPackets.add(NetworkProfiler.getProcessRxPackets());
                    wifiTxPackets.add(NetworkProfiler.getProcessTxPackets());
                    wifiTxBytes.add(NetworkProfiler.getProcessTxBytes());

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        t.start();
    }

    private void calculatePacketRate() {
        for (int i = 0; i < wifiRxPackets.size() - 1; i++)
            wifiRxPackets.set(i, wifiRxPackets.get(i + 1) - wifiRxPackets.get(i));

        for (int i = 0; i < wifiTxPackets.size() - 1; i++)
            wifiTxPackets.set(i, wifiTxPackets.get(i + 1) - wifiTxPackets.get(i));
    }

    private void calculateUplinkDataRate() {
        for (int i = 0; i < wifiTxBytes.size() - 1; i++) {
            wifiTxBytes.set(i, wifiTxBytes.get(i + 1) - wifiTxBytes.get(i));
        }
    }


    private byte timeoutDchFach = 6; // Inactivity timer for transition from DCH -> FACH
    private byte timeoutFachIdle = 4; // Inactivity timer for transition from FACH -> IDLE
    private int uplinkThreshold = 151;
    private int downlikThreshold = 119;
    private byte threegState = THREEG_IN_IDLE_STATE;
    private boolean fromIdleState = true;
    private boolean fromDchState = false;
    private long prevRxBytes, prevTxBytes;

    private void calculate3GStates() {
        Thread t = new Thread() {
            public void run() {

                while (!stopEstimatingEnergy) {

                    switch (threegState) {
                        case THREEG_IN_IDLE_STATE:
                            threegIdleState();
                            break;
                        case THREEG_IN_FACH_STATE:
                            threegFachState();
                            break;
                        case THREEG_IN_DCH_STATE:
                            threegDchState();
                            break;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        t.start();
    }

    private void threegIdleState() {
        int dataActivity = telephonyManager.getDataActivity();

        if (dataActivity == TelephonyManager.DATA_ACTIVITY_IN
                || dataActivity == TelephonyManager.DATA_ACTIVITY_OUT
                || dataActivity == TelephonyManager.DATA_ACTIVITY_INOUT) {
            // 3G is in the FACH state because is sending or receiving data
            Log.d(TAG, "3G in FACH state from IDLE");
            threegState = THREEG_IN_FACH_STATE;
            fromIdleState = true;
            threegFachState();
            return;
        }

        Log.d(TAG, "3G in IDLE state");
        // 3G is in the IDLE state
        threeGActiveState.add(THREEG_IN_IDLE_STATE);
    }

    private void threegFachState() {
        if (fromIdleState || fromDchState) {
            // The FACH state is just entered from IDLE or DCH, we should stay here at least 1 second
            // to measure the size of the buffer and in case to transit in DCH in the next second
            fromIdleState = false;
            fromDchState = false;
            prevRxBytes = NetworkProfiler.getProcessRxBytes();
            prevTxBytes = NetworkProfiler.getProcessTxBytes();
        } else { // 3G was in FACH
            if (timeoutFachIdle == 0) {
                Log.d(TAG, "3G in IDLE state from FACH");
                timeoutFachIdle = 4;
                threegState = THREEG_IN_IDLE_STATE;
                threegIdleState();
                return;
            } else if (telephonyManager.getDataActivity() == TelephonyManager.DATA_ACTIVITY_NONE) {
                Log.d(TAG, "3G in FACH state with no data activity");
                timeoutFachIdle--;
            } else
                timeoutFachIdle = 4;

            if ((NetworkProfiler.getProcessRxBytes() - prevRxBytes) > downlikThreshold
                    || (NetworkProfiler.getProcessTxBytes() - prevTxBytes) > uplinkThreshold) {
                Log.d(TAG, "3G in DCH state from FACH");
                timeoutFachIdle = 4;
                threegState = THREEG_IN_DCH_STATE;
                threegDchState();
                return;
            }
        }

        Log.d(TAG, "3G in FACH state");
        threeGActiveState.add(THREEG_IN_FACH_STATE);
    }

    private void threegDchState() {
        if (timeoutDchFach == 0) {
            Log.d(TAG, "3G in FACH state from DCH");
            timeoutDchFach = 6;
            threegState = THREEG_IN_FACH_STATE;
            fromDchState = true;
            threegFachState();
            return;
        } else if (telephonyManager.getDataActivity() == TelephonyManager.DATA_ACTIVITY_NONE) {
            Log.d(TAG, "3G in DCH state with no data activity");
            timeoutDchFach--;
        } else
            timeoutDchFach = 6;

        Log.d(TAG, "3G in DCH state");
        threeGActiveState.add(THREEG_IN_DCH_STATE);
    }

    public int getWiFiRxPacketRate(int i) {
        try {
            return wifiRxPackets.get(i).intValue();
        } catch (Exception e) {
            Log.w(TAG, "Could not get WiFiRxPacketRate: " + e);
            return 0;
        }
    }

    public int getWiFiTxPacketRate(int i) {
        try {
            return wifiTxPackets.get(i).intValue();
        } catch (Exception e) {
            Log.w(TAG, "Could not get WiFiTxPacketRate: " + e);
            return 0;
        }
    }

    public boolean noConnectivity() {
        return (connectivityManager.getActiveNetworkInfo()) == null;
    }

    public int getLinkSpeed() {
        if (wifiManager == null) {
            wifiManager = (WifiManager)
                    context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getLinkSpeed();
    }

    public byte get3GActiveState(int i) {
        if (threeGActiveState == null || threeGActiveState.size() <= i) {
            return 0;
        }
        if (isIndexValid(threeGActiveState, i)) {
            return threeGActiveState.get(i);
        }
        return 0;
    }

    public long getUplinkDataRate(int i) {
        if (isIndexValid(wifiTxBytes, i)) {
            return wifiTxBytes.get(i);
        } else {
            Log.w(TAG, "Could not get WiFiTxBytes");
            return 0;
        }
    }

    private boolean isIndexValid(List l, int i) {
        return l != null && i >= 0 && i < l.size();
    }

    /**
     * @param rtt
     */
    public static void setRtt(int rtt) {
        NetworkProfiler.rtt = rtt;
    }

    /**
     * @param ulRate
     */
    public static void setUlRate(int ulRate) {
        lastUlRate = new NetworkBWRecord(ulRate, System.currentTimeMillis());
        ulRateHistory.add(lastUlRate);
    }

    /**
     * @param dlRate
     */
    public static void setDlRate(int dlRate) {
        lastDlRate = new NetworkBWRecord(dlRate, System.currentTimeMillis());
        dlRateHistory.add(lastDlRate);
    }

    public void onDestroy() {
        try {
            context.unregisterReceiver(networkStateReceiver);
        } catch (IllegalArgumentException e) {
            Log.v(TAG, "The receiver was not registered, no necessary to unregister.");
        }
    }

    public static void measureRtt(String serverIp, int serverPort) {
        Socket clientSocket = new Socket();
        try {
            clientSocket.connect(new InetSocketAddress(serverIp, serverPort), 1000);

            try (OutputStream os = clientSocket.getOutputStream();
                 InputStream is = clientSocket.getInputStream();
                 DataInputStream dis = new DataInputStream(is)) {

                rttPing(is, os);
            } catch (IOException e) {
                Log.w(TAG, "Could not connect with the VM for measuring the RTT: " + e);
            }
        } catch (IOException e) {
            Log.d(TAG, "Could not connect to VM for RTT measuring: " + e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Could not close socket with VMM on RTT measuring: " + e);
            }
        }
    }

    public static NetworkBWRecord measureDlRate(String serverIp, int serverPort) {

        OutputStream os = null;
        InputStream is = null;
        DataInputStream dis = null;

        long time = 0;
        long rxBytes = 0;

        try {
            final Socket clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(serverIp, serverPort), 1000);
            os = clientSocket.getOutputStream();
            is = clientSocket.getInputStream();
            dis = new DataInputStream(is);

            os.write(RapidMessages.DOWNLOAD_FILE);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    long t0 = System.nanoTime();
                    long elapsed = 0;
                    while (elapsed < 3000) {
                        try {
                            Thread.sleep(3000 - elapsed);
                        } catch (InterruptedException e1) {
                            Thread.currentThread().interrupt();
                        } finally {
                            elapsed = (System.nanoTime() - t0) / 1000000;
                        }
                    }
                    RapidUtils.closeQuietly(clientSocket);
                }
            }).start();

            time = System.nanoTime();
            // rxBytes = NetworkProfiler.getProcessRxBytes();
            while (true) {
                rxBytes += is.read(buffer);
                os.write(1);
            }
        } catch (UnknownHostException e) {
            Log.w(TAG, "UnknownHostException while measuring download rate: " + e);
        } catch (SocketException e) {
            Log.w(TAG, "Finished the download rate measurement");
        } catch (IOException e) {
            Log.w(TAG, "IOException while measuring download rate: " + e);
        } finally {

            time = System.nanoTime() - time;
            // rxBytes = NetworkProfiler.getProcessRxBytes() - rxBytes;

            if (os != null) {

                // If the streams are null it means that no measurement was performed
                addNewDlRateEstimate(rxBytes, time);

                RapidUtils.closeQuietly(os);
                RapidUtils.closeQuietly(is);
                RapidUtils.closeQuietly(dis);
            }
        }
        return lastDlRate;
    }

    public static NetworkBWRecord measureUlRate(String serverIp, int serverPort) {

        OutputStream os = null;
        InputStream is = null;
        DataInputStream dis = null;

        long txTime;
        long txBytes;

        Socket clientSocket = null;
        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(serverIp, serverPort), 1000);
            os = clientSocket.getOutputStream();
            is = clientSocket.getInputStream();
            dis = new DataInputStream(is);

            Log.i(TAG, "Connected to VM. Sending request for UL rate measurement");
            os.write(RapidMessages.UPLOAD_FILE);

            // This will throw an exception when the VM will close the socket after 3 seconds
            Log.i(TAG, "UL rate measurement, waiting the VM to send the OK byte...");
            long start = System.nanoTime();
            while ((System.nanoTime() - start) / 1000000000 < 3) {
                os.write(buffer);
            }
            Log.i(TAG, "Finished upload measurement.");

        } catch (UnknownHostException e) {
            Log.w(TAG, "UnknownHostException while measuring upload rate: " + e);
        } catch (SocketException e) {
            Log.w(TAG, "Finished sending data for 3s for upload rate measurement");
        } catch (IOException e) {
            Log.w(TAG, "IOException while measuring upload rate: " + e);
        } finally {
            RapidUtils.closeQuietly(os);
            RapidUtils.closeQuietly(is);
            RapidUtils.closeQuietly(dis);
            RapidUtils.closeQuietly(clientSocket);

            try {
                Log.w(TAG, "Asking the VM to tell us how many data it actually received in 3s");
                clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(config.getClone().getIp(), config.getClonePortBandwidthTest()), 1000);
                os = clientSocket.getOutputStream();
                is = clientSocket.getInputStream();
                dis = new DataInputStream(is);

                os.write(RapidMessages.UPLOAD_FILE_RESULT);
                txBytes = dis.readLong();
                txTime = dis.readLong();

                addNewUlRateEstimate(txBytes, txTime);
            } catch (UnknownHostException e) {
                Log.w(TAG, "UnknownHostException while getting the upload rate result from the VM: " + e);
            } catch (IOException e) {
                Log.w(TAG, "IOException while getting the upload rate result from the VM: " + e);
            } catch (Exception e) {
                Log.w(TAG, "Exception while getting the upload rate result from the VM: " + e);
            } finally {
                RapidUtils.closeQuietly(os);
                RapidUtils.closeQuietly(is);
                RapidUtils.closeQuietly(dis);
                RapidUtils.closeQuietly(clientSocket);
            }
        }
        return lastUlRate;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
