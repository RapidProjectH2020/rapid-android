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

import android.os.Debug;
import android.util.Log;
import dalvik.bytecode.Opcodes;

// Removed VMDebug since it cannot be imported for android >= 2.3
//import dalvik.system.VMDebug;

public class ProgramProfiler {
	
	private static final String TAG = "ProgramProfiler";
	
	public String appName;
	public String methodName;
	public Long execTime;
	public Long threadCpuTime;
	
	public int threadAllocSize;
	public int instructionCount;
	public int methodInvocationCount;
	public int gcThreadInvocationCount;
	public int gcGlobalInvocationCount;

	private PrivateInstructionCount mIcount;
	private Long mStartTime;
	private int mStartThreadAllocSize;
	private Long mStartThreadCpuTime;
	private int mStartThreadGcInvocationCount;
	private int mStartGlobalGcInvocationCount;

	private static Integer profilersRunning;
	private static Boolean memAllocTrackerRunning;

	public ProgramProfiler() {
		methodName = "";
		mIcount = new PrivateInstructionCount();
		if (memAllocTrackerRunning == null) {
			memAllocTrackerRunning = false;
			profilersRunning = 0;
		}
	}

	public ProgramProfiler(String appName, String mName) {
		this.appName = appName;
		methodName = mName;
		mIcount = new PrivateInstructionCount();
		if (memAllocTrackerRunning == null) {
			memAllocTrackerRunning = false;
			profilersRunning = 0;
		}
	}

	public void startExecutionInfoTracking() {
		mStartTime = System.nanoTime();
		mStartThreadCpuTime = Debug.threadCpuTimeNanos();

		if (memAllocTrackerRunning == false) {
			Debug.startAllocCounting();
			memAllocTrackerRunning = true;
		}
		mStartThreadAllocSize = Debug.getThreadAllocSize();
		mStartThreadGcInvocationCount = Debug.getThreadGcInvocationCount();
		mStartGlobalGcInvocationCount = Debug.getGlobalGcInvocationCount();

		mIcount.startInstructionCounting();
		profilersRunning++;
	}

	public void stopAndCollectExecutionInfoTracking() {
		profilersRunning--;
		mIcount.stopInstructionCounting();
		instructionCount = mIcount.instructionsExecuted;
		methodInvocationCount = mIcount.methodsExecuted;
		threadAllocSize = Debug.getThreadAllocSize() - mStartThreadAllocSize;
		gcThreadInvocationCount = Debug.getThreadGcInvocationCount() - mStartThreadGcInvocationCount;
		gcGlobalInvocationCount = Debug.getGlobalGcInvocationCount() - mStartGlobalGcInvocationCount;
		
		if (profilersRunning == 0) {
			Debug.stopAllocCounting();
			memAllocTrackerRunning = false;
		}

		threadCpuTime = Debug.threadCpuTimeNanos() - mStartThreadCpuTime;
		execTime = System.nanoTime() - mStartTime;

		Log.d(TAG, methodName + ": Thread Alloc Size - "
				+ (Debug.getThreadAllocSize() - mStartThreadAllocSize));
		Log.d(TAG, methodName
				+ "Total instructions executed: " + instructionCount
				+ " Method invocations: " + methodInvocationCount + "in "
				+ execTime / 1000000 + "ms");
	}

	/**
	 * A wrapper class for VMDebug for instruction counting. Needed because of
	 * issues (probably a bug) in the original debugging library, when counters
	 * are not correctly altered.
	 * 
	 * Only start InstructionCounting once, if none is running and only stop
	 * when the last monitoring method is exiting.
	 * 
	 */
	private static class PrivateInstructionCount {
		private static final int NUM_INSTR = 256;

		private int[] mCounts;
		private int mStartInstructions;
		private int mStartMethods;
		private static int localProfilersRunning;

		public int instructionsExecuted;
		public int methodsExecuted;

		public PrivateInstructionCount() {
			mCounts = new int[NUM_INSTR];
			instructionsExecuted = 0;
			methodsExecuted = 0;
		}

		/**
		 * Start InstructionCounting by taking note of current VM instruction
		 * count and starting VMDebug InstructionCounting if none is running
		 */
		public void startInstructionCounting() {
			// Sokol: uncomment if switching to android 2.1
//			if (localProfilersRunning == 0)
//				VMDebug.startInstructionCounting();
			if (collect()) {
				mStartInstructions = globalTotal();
				mStartMethods = globalMethodInvocations();
			}
			localProfilersRunning++;
		}

		/**
		 * Stop InstructionCounting by collecting the number of instructions
		 * executed and stopping VMDebug InstructionCounting if it is the last
		 * profiler wanting the data.
		 * 
		 * @return instructions since performing startInstructionCounting()
		 */
		public void stopInstructionCounting() {
			localProfilersRunning--;
			// Sokol: uncomment if switching to android 2.1
//			if (localProfilersRunning == 0)
//				VMDebug.stopInstructionCounting();
			if (collect()) {
				instructionsExecuted = globalTotal() - mStartInstructions;
				methodsExecuted = globalMethodInvocations() - mStartMethods;
			}
		}

		/**
		 * Method copied from android.os.Debug.InstructionCount class, for loop
		 * changed to supposedly faster format
		 * 
		 * @return number of instructions executed globally
		 */
		public int globalTotal() {
			int count = 0;
			for (int c : mCounts) {
				count += c;
			}

			return count;
		}

		/**
		 * Method copied from android.os.Debug.InstructionCount
		 * 
		 * @return
		 */
		public int globalMethodInvocations() {
			int count = 0;
			// count += mCounts[Opcodes.OP_EXECUTE_INLINE];
			count += mCounts[Opcodes.OP_INVOKE_VIRTUAL];
			count += mCounts[Opcodes.OP_INVOKE_SUPER];
			count += mCounts[Opcodes.OP_INVOKE_DIRECT];
			count += mCounts[Opcodes.OP_INVOKE_STATIC];
			count += mCounts[Opcodes.OP_INVOKE_INTERFACE];
			count += mCounts[Opcodes.OP_INVOKE_VIRTUAL_RANGE];
			count += mCounts[Opcodes.OP_INVOKE_SUPER_RANGE];
			count += mCounts[Opcodes.OP_INVOKE_DIRECT_RANGE];
			count += mCounts[Opcodes.OP_INVOKE_STATIC_RANGE];
			count += mCounts[Opcodes.OP_INVOKE_INTERFACE_RANGE];
			// count += mCounts[Opcodes.OP_INVOKE_DIRECT_EMPTY];
			count += mCounts[Opcodes.OP_INVOKE_VIRTUAL_QUICK];
			count += mCounts[Opcodes.OP_INVOKE_VIRTUAL_QUICK_RANGE];
			count += mCounts[Opcodes.OP_INVOKE_SUPER_QUICK];
			count += mCounts[Opcodes.OP_INVOKE_SUPER_QUICK_RANGE];
			return count;
		}

		/**
		 * Method copied from android.os.Debug.InstructionCount
		 * 
		 * @return
		 */
		private boolean collect() {
			// Sokol: uncomment if switching to android 2.1
//			try {
//				VMDebug.getInstructionCount(mCounts);
//			} catch (UnsupportedOperationException uoe) {
//				return false;
//			}
//			return true;
			
			return false;
		}
	}
}
