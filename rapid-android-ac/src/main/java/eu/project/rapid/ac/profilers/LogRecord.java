/*******************************************************************************
 * Copyright (C) 2015, 2016 RAPID EU Project
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *******************************************************************************/
package eu.project.rapid.ac.profilers;

import eu.project.rapid.common.RapidConstants;

/**
 * Log record of the profiler.
 * 
 * Structure of a log record:
 * 
 * Method Name, Execution Location, PrepareDataDuration (nanoseconds), Execution Duration (nanoseconds), Execution
 * Duration (excl. overheads),Thread CPU time,Instruction Count,Method
 * Invocation Count,Thread Allocation Size,Garbage Collector invocation count
 * (thread),Garbage Collector invocation count (global),Current Network
 * Type,Current Network Subtype,Current RTT,Current UlRate, Current DlRate, Bytes
 * Received(RX),Bytes Transmitted(TX),Battery Voltage Change,Timestamp
 * 
 */
public class LogRecord {
	String appName;
	public static String methodName;
	RapidConstants.ExecLocation execLocation;
	long prepareDataDuration;
	long execDuration;
	long pureDuration;
	long energyConsumption;
	double cpuEnergy;
	double screenEnergy;
	double wifiEnergy;
	double threeGEnergy;
	
	private Long threadCpuTime;
	private int instructionCount;
	private int methodCount;
	private int threadAllocSize;
	private int threadGcInvocationCount;
	private int globalGcInvocationCount;

	String networkType;
	String networkSubtype;
	private int rtt;
	int ulRate = -1;
	int dlRate = -1;
	private long rxBytes = -1;
	private long txBytes = -1;

	private long batteryVoltageChange;

    static final String LOG_HEADERS = "#MethodName,ExecLocation,PrepareDataDuration,ExecDuration,PureDuration," +
			"EnergyConsumption,CpuEnergy,ScreenEnergy,WifiEnergy,3GEnergy," +
			"ThreadCpuTime,InstructionCount,MethodCount,ThreadAllocSize,ThreadGcInvocCount,GlobalGcInvocCount," +
			"NetType,NetSubtype,RTT,UlRate,DlRate,RxBytes,TxBytes," +
			"BatteryVoltChange,LogRecordTime";

	/**
	 * Collect readings of the different profilers together from the different
	 * running profilers
	 * 
	 * @param progProfiler
	 *            instace of ProgramProfiler
	 * @param netProfiler
	 *            instance of NetworkProfiler
	 * @param devProfiler
	 *            instance of DeviceProfiler
	 */
	LogRecord(ProgramProfiler progProfiler, NetworkProfiler netProfiler,
			  DeviceProfiler devProfiler) {
		appName = progProfiler.appName;
		methodName = progProfiler.methodName;
		execDuration = progProfiler.execTime;
		threadCpuTime = progProfiler.threadCpuTime;

		instructionCount = progProfiler.instructionCount;
		methodCount = progProfiler.methodInvocationCount;

		threadAllocSize = progProfiler.threadAllocSize;
		threadGcInvocationCount = progProfiler.gcThreadInvocationCount;
		globalGcInvocationCount = progProfiler.gcGlobalInvocationCount;

		networkType = NetworkProfiler.currentNetworkTypeName;
		networkSubtype = NetworkProfiler.currentNetworkSubtypeName;
		rtt = NetworkProfiler.rtt;
		
		if (NetworkProfiler.lastUlRate != null)
			ulRate = NetworkProfiler.lastUlRate.getBw();
		
		if (NetworkProfiler.lastDlRate != null)
			dlRate = NetworkProfiler.lastDlRate.getBw();
		
		if (netProfiler != null) {
			rxBytes = netProfiler.rxBytes;
			txBytes = netProfiler.txBytes;
		} else {
			rxBytes = -1;
			txBytes = -1;
		}

		batteryVoltageChange = devProfiler.batteryVoltageDelta;
	}

	/**
	 * Convert the log record to string for storing
	 */
	public String toString() {

        long logRecordTime = System.currentTimeMillis();
		String progProfilerRecord = methodName + "," + execLocation + "," + prepareDataDuration + ","
				+ execDuration + "," + pureDuration + "," + energyConsumption + "," + 
				cpuEnergy + "," + screenEnergy + "," + wifiEnergy + "," + threeGEnergy
				+ "," + threadCpuTime + ","
				+ instructionCount + "," + methodCount + "," + threadAllocSize
				+ "," + threadGcInvocationCount + "," + globalGcInvocationCount;

		String netProfilerRecord = " , , , , , , ";

		if (execLocation.equals(RapidConstants.ExecLocation.REMOTE)) {
			netProfilerRecord = networkType + ", " + networkSubtype + "," + rtt
					+ "," + ulRate + "," + dlRate + "," + rxBytes + "," + txBytes;
		}

		String devProfilerRecord = "" + batteryVoltageChange;
		
		return progProfilerRecord + "," + netProfilerRecord + ","
				+ devProfilerRecord + "," + logRecordTime;
	}
}
