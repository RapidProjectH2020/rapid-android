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
package eu.project.rapid.ac.utils;

import android.os.Environment;

import java.io.File;

public class Constants {

  public static final int MAX_NUM_CLIENTS = 32;

  // The number of recent method executions to keep in DB so that they can be used for offloading
  // decision.
  public static final int MAX_METHOD_EXEC_HISTORY = 50;

  // TODO: check the real device name as returned by android for the HTC G1 phone
  public static final String PHONE_NAME_HTC_G1 = "HTC G1";
  public static final String PHONE_MODEL_HTC_DESIRE = "HTC Desire";
  public static final String PHONE_MODEL_SAMSUNG_GALAXY_S = "samsung GT-I9000";
  public static final String PHONE_MODEL_MOTOROLA_MOTO_G = "Motorola Moto G";

  public static final String RAPID_SETTINGS = "rapid_settings";
  public static final String MY_OLD_ID = "MY_OLD_ID";
  public static final String PREV_VM_IP = "PREV_VM_IP";
  public static final String PREV_VMM_IP = "PREV_VMM_IP";
  public static final String MY_OLD_ID_WITH_DS = "MY_OLD_ID_WITH_DS";

  public static final String MNT_SDCARD =
      Environment.getExternalStorageDirectory().getAbsolutePath();

  public static final String RAPID_FOLDER = MNT_SDCARD + File.separator + "rapid";
  public static final String CLONE_CONFIG_FILE = RAPID_FOLDER + File.separator + "config-clone.cfg";
  public static final String PHONE_CONFIG_FILE = RAPID_FOLDER + File.separator + "config-phone.cfg";
  public static final String TEST_LOGS_FOLDER =
      RAPID_FOLDER + File.separator + "test_logs" + File.separator;
  public static final String LOG_FILE_NAME = RAPID_FOLDER + File.separator + "rapid-log-";
  public static final String SSL_KEYSTORE = RAPID_FOLDER + File.separator + "keystore.bks";
  public static final String SSL_CA_TRUSTSTORE =
      RAPID_FOLDER + File.separator + "ca_truststore.bks";
  public static final String SSL_CERT_ALIAS = "cert";
  public static final String SSL_DEFAULT_PASSW = "changeme";

  public static final String FILE_OFFLOADED = RAPID_FOLDER + File.separator + "offloaded";
  public static final String FILE_D2D_PHONES = RAPID_FOLDER + File.separator + "d2d-phones-set.ser";
  public static final String FILE_DB_CACHE = RAPID_FOLDER + File.separator + "dbCache-";
  public static final String CLONE_ID_FILE = RAPID_FOLDER + File.separator + "cloneId";

  public static final String VIRUS_DB_PATH = RAPID_FOLDER + File.separator + "virusDB";
  public static final String VIRUS_FOLDER_TO_SCAN =
      RAPID_FOLDER + File.separator + "virusFolderToScan_big";
  public static final String VIRUS_FILES_EXCLUDE_ZIP =
      RAPID_FOLDER + File.separator + "filesExcludeZip.txt";
  public static final String VIRUS_FOLDER_TAR_GZ = VIRUS_FOLDER_TO_SCAN + ".tar.gz";
  public static final String VIRUS_FOLDER_ZIP = VIRUS_FOLDER_TO_SCAN + ".zip";

  public static final int D2D_BROADCAST_PORT = 7654;
  public static final int D2D_BROADCAST_INTERVAL = 60 * 1000; // frequency (ms) to broadcast the
                                                              // hello message
}
