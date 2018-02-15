package eu.project.rapid.gvirtus4a;

import java.io.IOException;

import eu.project.rapid.gvirtus4a.params.IntParam;
import eu.project.rapid.gvirtus4a.params.StringParam;

public class CudaRtFrontend {

	private static final String LOG_TAG = "CUDA RUNTIME FRONTEND";
	
	private Frontend frontend;
	
	public CudaRtFrontend(String serverIpAddress ,  int port) {

		frontend=Frontend.getFrontend(serverIpAddress, port);

	}

	/*
	public int Execute(String routine) throws IOException {

		int exit_code = Frontend.Execute(routine);
		return exit_code;

	}
	*/

	@Override
	public void finalize() {
		close();
	}

	public void close() {
		frontend.close();
		frontend=null;
	}
	/* CUDA RUNTIME DEVICE */
	
	public int cudaGetDeviceCount(IntParam result) throws IOException {
		Buffer buffer=new Buffer();
		buffer.AddPointer(0);
		String outputbuffer = "";
		int exit_c = frontend.Execute("cudaGetDeviceCount",buffer);
		if (exit_c!=0) { return exit_c; }
		int sizeType = frontend.readByte();
		frontend.readBytes(7);
		for (int i = 0; i < sizeType; i++) {
			if (i == 0) {
				byte bb = frontend.readByte();
				outputbuffer += Integer.toHexString(bb & 0xFF);
			} else
				frontend.readByte();
		}
		StringBuilder out2 = new StringBuilder();
		if (outputbuffer.length() > 2) {
			for (int i = 0; i < outputbuffer.length() - 1; i += 2) {
				String str = outputbuffer.substring(i, i + 2);
				out2.insert(0, str);
			}
			outputbuffer = String.valueOf(Integer.parseInt(out2.toString(), 16));
		}
		System.out.println("Integer.valueOf(outputbuffer): " + Integer.valueOf(outputbuffer));
		//count=Integer.valueOf(outputbuffer);
		//count=new Integer(outputbuffer);
		result.value=Integer.valueOf(outputbuffer);
		return exit_c;
	}

	public int cudaDeviceCanAccessPeer( int device, int peers, IntParam result) throws IOException {
		Buffer buffer=new Buffer();
		buffer.AddPointer(0);
		buffer.AddInt(device);
		buffer.AddInt(peers);
		String outputbuffer = "";
		int exit_c =  frontend.Execute("cudaDeviceCanAccessPeer",buffer );
		//  ExecuteMultiThread("cudaDeviceCanAccessPeer",b, );
		if (exit_c!=0) { return exit_c; }
		int sizeType = frontend.readByte();
		frontend.readBytes(7);
		
		for (int i = 0; i < sizeType; i++) {
			if (i == 0) {
				byte bb = frontend.readByte();
				outputbuffer += Integer.toHexString(bb & 0xFF);
			} else
				frontend.readByte();
		}
		StringBuilder out2 = new StringBuilder();
		if (outputbuffer.length() > 2) {
			for (int i = 0; i < outputbuffer.length() - 1; i += 2) {
				String str = outputbuffer.substring(i, i + 2);
				out2.insert(0, str);
			}
			outputbuffer = String.valueOf(Integer.parseInt(out2.toString(), 16));
		}
		result.value=Integer.valueOf(outputbuffer);
		return exit_c;
	}

	public int cudaDriverGetVersion(IntParam result) throws IOException {
		Buffer buffer=new Buffer();
		buffer.AddPointer(0);
		String outputbuffer = "";
		int exit_c =  frontend.Execute("cudaDriverGetVersion",buffer );
		//  ExecuteMultiThread("cudaDriverGetVersion",b, );
		if (exit_c!=0) { return exit_c; }
		int sizeType = frontend.readByte();
		frontend.readBytes(7);
		for (int i = 0; i < sizeType; i++) {
			if (i == 0 || i == 1) {
				byte bb = frontend.readByte();
				outputbuffer += Integer.toHexString(bb & 0xFF);
			} else
				frontend.readByte();
		}

		StringBuilder out2 = new StringBuilder();
		if (outputbuffer.length() > 2) {
			for (int i = 0; i < outputbuffer.length() - 1; i += 2) {
				String str = outputbuffer.substring(i, i + 2);
				out2.insert(0, str);
			}
			outputbuffer = String.valueOf(Integer.parseInt(out2.toString(), 16));
		}
		result.value=Integer.valueOf(outputbuffer);
		return exit_c;
	}

	public int cudaRuntimeGetVersion(IntParam result) throws IOException {

		Buffer buffer=new Buffer();
		buffer.AddPointer(0);
		String outputbuffer = "";
		int exit_c =  frontend.Execute("cudaRuntimeGetVersion",buffer );
		//  ExecuteMultiThread("cudaRuntimeGetVersion",b, );
		if (exit_c!=0) { return exit_c; }
		int sizeType = frontend.readByte();
		frontend.readBytes(7);
		for (int i = 0; i < sizeType; i++) {
			if (i == 0 || i == 1) {
				byte bb = frontend.readByte();
				outputbuffer += Integer.toHexString(bb & 0xFF);
			} else
				frontend.readByte();
		}
		StringBuilder out2 = new StringBuilder();
		if (outputbuffer.length() > 2) {
			for (int i = 0; i < outputbuffer.length() - 1; i += 2) {
				String str = outputbuffer.substring(i, i + 2);
				out2.insert(0, str);
			}
			outputbuffer = String.valueOf(Integer.parseInt(out2.toString(), 16));
		}
		result.value=Integer.valueOf(outputbuffer);
		return exit_c;
	}

	public int cudaSetDevice(int device) throws IOException {

		Buffer buffer=new Buffer();
		buffer.Add(device);
		int exit_c =  frontend.Execute("cudaSetDevice" ,buffer);
		if (exit_c!=0) { return exit_c; }
		//  ExecuteMultiThread("cudaSetDevice",b, );
		return exit_c;
	}

	public int cudaGetErrorString(int error, StringParam result) throws IOException {

		Buffer buffer=new Buffer();
		buffer.AddInt(error);
		String outbuffer = "";
		StringBuilder output = new StringBuilder();
		int exit_c =  frontend.Execute("cudaGetErrorString" ,buffer);
		if (exit_c!=0) { return exit_c; }
		int sizeType = frontend.readByte();
		// System.out.print("sizeType " + sizeType);

		frontend.readBytes(15);
		

		for (int i = 0; i < sizeType; i++) {
			byte bit = frontend.readByte();
			outbuffer += Integer.toHexString(bit);
			// System.out.print(outbuffer.toString());
		}
		for (int i = 0; i < outbuffer.length() - 1; i += 2) {
			String str = outbuffer.substring(i, i + 2);
			output.append((char) Integer.parseInt(str, 16));

		}
		result.value=output.toString();
		return exit_c;

	}

	public int cudaDeviceReset() throws IOException {
		Buffer buffer=new Buffer();
		int exit_c =  frontend.Execute("cudaDeviceReset" ,buffer);
		if (exit_c!=0) { return exit_c; }
		return exit_c;
	}

	public int cudaGetDeviceProperties( int device, CudaDeviceProp cudaDeviceProp) throws IOException {
		Buffer buffer=new Buffer();
		String outbuffer = "";
		StringBuilder output = new StringBuilder();
		

		buffer.AddStruct(cudaDeviceProp.getStruct());
		buffer.AddInt(device);
		int exit_c =  frontend.Execute("cudaGetDeviceProperties",buffer );
		if (exit_c!=0) { return exit_c; }
		frontend.readBytes(8);
		for (int i = 0; i < 256; i++) {
			byte bit = frontend.readByte();

			outbuffer += Integer.toHexString(bit);
		}
		for (int i = 0; i < outbuffer.length() - 1; i += 2) {
			String str = outbuffer.substring(i, i + 2);
			if (str.equals("00")) {
				break;
			}
			output.append((char) Integer.parseInt(str, 16));
		}
		cudaDeviceProp.setName(output.toString());
		cudaDeviceProp.setTotalGlobalMem(frontend.getLong());
		cudaDeviceProp.setSharedMemPerBlock(frontend.getLong());
		cudaDeviceProp.setRegsPerBlock(frontend.getInt());

		cudaDeviceProp.setWarpSize(frontend.getInt());
		cudaDeviceProp.setMemPitch(frontend.getLong());
		cudaDeviceProp.setMaxThreadsPerBlock(frontend.getInt());
		cudaDeviceProp.setMaxThreadsDim(frontend.getInt(),0);


		cudaDeviceProp.setMaxThreadsDim(frontend.getInt(),1);

		cudaDeviceProp.setMaxThreadsDim(frontend.getInt(), 2);
		cudaDeviceProp.setMaxGridSize(frontend.getInt(),0);
		cudaDeviceProp.setMaxGridSize(frontend.getInt(),1);
		cudaDeviceProp.setMaxGridSize(frontend.getInt(),2);
		cudaDeviceProp.setClockRate(frontend.getInt()); // check
		cudaDeviceProp.setTotalConstMem(frontend.getLong());
		cudaDeviceProp.setMajor(frontend.getInt());
		cudaDeviceProp.setMinor(frontend.getInt());
		cudaDeviceProp.setTextureAlignment(frontend.getLong());
		cudaDeviceProp.setTexturePitchAlignment(frontend.getLong()); // check
		cudaDeviceProp.setDeviceOverlap(frontend.getInt());
		cudaDeviceProp.setMultiProcessorCount(frontend.getInt());
		cudaDeviceProp.setKernelExecTimeoutEnabled(frontend.getInt());
		cudaDeviceProp.setIntegrated(frontend.getInt());
		cudaDeviceProp.setCanMapHostMemory(frontend.getInt());
		cudaDeviceProp.setComputeMode(frontend.getInt());
		cudaDeviceProp.setMaxTexture1D(frontend.getInt());
		cudaDeviceProp.setMaxTexture1DMipmap(frontend.getInt());
		cudaDeviceProp.setMaxTexture1DLinear(frontend.getInt()); // check
		cudaDeviceProp.setMaxTexture2D(frontend.getInt(),0);
		cudaDeviceProp.setMaxTexture2D(frontend.getInt(),1);

		cudaDeviceProp.setMaxTexture2DMipmap(frontend.getInt(),0);
		cudaDeviceProp.setMaxTexture2DMipmap(frontend.getInt(),1);

		cudaDeviceProp.setMaxTexture2DLinear(frontend.getInt(),0);
		cudaDeviceProp.setMaxTexture2DLinear(frontend.getInt(),1);
		cudaDeviceProp.setMaxTexture2DLinear(frontend.getInt(),2);

		cudaDeviceProp.setMaxTexture2DGather(frontend.getInt(),0);
		cudaDeviceProp.setMaxTexture2DGather(frontend.getInt(),1);

		cudaDeviceProp.setMaxTexture3D(frontend.getInt(),0);
		cudaDeviceProp.setMaxTexture3D(frontend.getInt(),1);
		cudaDeviceProp.setMaxTexture3D(frontend.getInt(),2);

		cudaDeviceProp.setMaxTexture3DAlt(frontend.getInt(),0);
		cudaDeviceProp.setMaxTexture3DAlt(frontend.getInt(),1);
		cudaDeviceProp.setMaxTexture3DAlt(frontend.getInt(),2);
		cudaDeviceProp.setMaxTextureCubemap(frontend.getInt());
		cudaDeviceProp.setMaxTexture1DLayered(frontend.getInt(),0);
		cudaDeviceProp.setMaxTexture1DLayered(frontend.getInt(),1);
		cudaDeviceProp.setMaxTexture2DLayered(frontend.getInt(),0);
		cudaDeviceProp.setMaxTexture2DLayered(frontend.getInt(),1);
		cudaDeviceProp.setMaxTexture2DLayered(frontend.getInt(),2);
		cudaDeviceProp.setMaxTextureCubemapLayered(frontend.getInt(),0);
		cudaDeviceProp.setMaxTextureCubemapLayered(frontend.getInt(),1);
		cudaDeviceProp.setMaxSurface1D(frontend.getInt());
		cudaDeviceProp.setMaxSurface2D(frontend.getInt(),0);
		cudaDeviceProp.setMaxSurface2D(frontend.getInt(),1);
		cudaDeviceProp.setMaxSurface3D(frontend.getInt(),0);
		cudaDeviceProp.setMaxSurface3D(frontend.getInt(),1);
		cudaDeviceProp.setMaxSurface3D(frontend.getInt(),2);
		cudaDeviceProp.setMaxSurface1DLayered(frontend.getInt(),0);
		cudaDeviceProp.setMaxSurface1DLayered(frontend.getInt(),1);
		cudaDeviceProp.setMaxSurface2DLayered(frontend.getInt(),0);
		cudaDeviceProp.setMaxSurface2DLayered(frontend.getInt(),1);
		cudaDeviceProp.setMaxSurface2DLayered(frontend.getInt(),2);
		cudaDeviceProp.setMaxSurfaceCubemap(frontend.getInt());
		cudaDeviceProp.setMaxSurfaceCubemapLayered(frontend.getInt(),0);
		cudaDeviceProp.setMaxSurfaceCubemapLayered(frontend.getInt(),1);
		cudaDeviceProp.setSurfaceAlignment(frontend.getLong());
		cudaDeviceProp.setConcurrentKernels(frontend.getInt());
		cudaDeviceProp.setECCEnabled(frontend.getInt());
		cudaDeviceProp.setPciBusID(frontend.getInt());
		cudaDeviceProp.setPciDeviceID(frontend.getInt());
		cudaDeviceProp.setPciDomainID(frontend.getInt());
		cudaDeviceProp.setTccDriver(frontend.getInt());
		cudaDeviceProp.setAsyncEngineCount(frontend.getInt());
		cudaDeviceProp.setUnifiedAddressing(frontend.getInt());
		cudaDeviceProp.setMemoryClockRate(frontend.getInt());
		cudaDeviceProp.setMemoryBusWidth(frontend.getInt());
		cudaDeviceProp.setL2CacheSize(frontend.getInt());
		cudaDeviceProp.setMaxThreadsPerMultiProcessor(frontend.getInt());
		cudaDeviceProp.setStreamPrioritiesSupported(frontend.getInt());
		cudaDeviceProp.setGlobalL1CacheSupported(frontend.getInt());
		cudaDeviceProp.setLocalL1CacheSupported(frontend.getInt());
		cudaDeviceProp.setSharedMemPerMultiprocessor(frontend.getLong());
		cudaDeviceProp.setRegsPerMultiprocessor(frontend.getInt());
		cudaDeviceProp.setManagedMemory(frontend.getInt());
		cudaDeviceProp.setIsMultiGpuBoard(frontend.getInt());
		cudaDeviceProp.setMultiGpuBoardGroupID(frontend.getInt());
		//frontend.getInt(); // è in più da capire il perche'
		frontend.flush();
		return exit_c;
	}
}
