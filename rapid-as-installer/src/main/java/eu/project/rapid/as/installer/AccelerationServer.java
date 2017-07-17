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
package eu.project.rapid.as.installer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import eu.project.rapid.ac.utils.Constants;
import eu.project.rapid.ac.utils.Utils;
import eu.project.rapid.common.RapidUtils;

/**
 * App that downloads the AS and installs it on the emulator.
 * FIXME: Doesn't work if the app is not compiled with the Android OS because of the INSTALL_PACKAGES
 * permission.
 */
public class AccelerationServer extends Service {

    private static final String TAG = "AS-Installer";
    static String arch = System.getProperty("os.arch");

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
        Context context = this.getApplicationContext();
        if (context == null) {
            Log.e(TAG, "Context is null!!!");
            stopSelf();
        }

        // Create a special file on the clone that methods can use to check
        // if are being executed on the clone or on the phone.
        // This can be of help to advanced developers.
        if (createOrGetRapidFolder()) {
            Log.i(TAG, "Created RAPID folder: " + Constants.RAPID_FOLDER);
        } else {
            Log.e(TAG, "Error while creating RAPID folder: " + Constants.RAPID_FOLDER);
        }

        // Connect to the manager to register and get the configuration details
        Thread t = new Thread(new AsInstallHandler());
        t.start();

        return START_STICKY;
    }

    private boolean createOrGetRapidFolder() {
        File f = new File(Constants.RAPID_FOLDER);
        return f.exists() || f.mkdirs();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Read the config file to get the IP and port for the Manager.
     */
    private class AsInstallHandler implements Runnable {

        @Override
        public void run() {
            waitForNetworkToBeUp();

            Log.i(TAG, "Downloading the AS apk file from the outside server...");
            String rapidFolder = "/mnt/sdcard/rapid";
            String asPackage = "eu.project.rapid.as";
            String asActivity = "RapidServerActivity";
            String asFileName = "rapid-android-as-debug.apk";
            String asApkFileRemote = "http://www.gjanica.al/rapid/" + asFileName;
            String asApkFileLocal = rapidFolder + File.separator + asFileName;

            String cmdDownloadApkFile = "/system/xbin/wget " + asApkFileRemote + " -O " + asApkFileLocal;
            String cmdInstallApkFile = "/system/bin/pm install -r " + asApkFileLocal;
            String cmdStartAsActivity = "/system/bin/am start " + asPackage + "/." + asActivity;

            if (Utils.executeAndroidShellCommand(TAG, cmdDownloadApkFile, false) != 0) {
                Log.e(TAG, "Error while running the download command");
            } else {
                if (Utils.executeAndroidShellCommand(TAG, cmdInstallApkFile, false) != 0) {
                    Log.e(TAG, "Error while running the install command");
                } else {
                    if (Utils.executeAndroidShellCommand(TAG, cmdStartAsActivity, false) != 0) {
                        Log.e(TAG, "Error while running the start command");
                    }
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

            boolean internetReachable = false;
            do {
                try {
                    InetAddress testAddress = InetAddress.getByName("83.235.169.221");
                    try {
                        Log.i(TAG,
                                "Trying to ping the testing server: " + testAddress.getHostAddress());
                        internetReachable = testAddress.isReachable(5000);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "Error while trying to ping the testing server: " + e);
                    }
                } catch (UnknownHostException e1) {
                    Log.e(TAG, "Error while getting hostname: " + e1);
                }
            } while (!internetReachable);
            Log.i(TAG, "The testing server replied to ping. Network interface is up and running.");

            return vmIpAddress.getHostAddress();
        }
    }
}
