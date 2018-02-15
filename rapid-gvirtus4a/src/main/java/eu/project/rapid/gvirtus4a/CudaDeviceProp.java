package eu.project.rapid.gvirtus4a;


public class CudaDeviceProp {

	public static final String LOG_TAG="CUDA_DEVICE_PROPERTY";

    private int runtimeVersion;

	private String name;
	private long totalGlobalMem = 0;
	/** < Global memory available on device in bytes */
	private long sharedMemPerBlock = 0;
	/** < Shared memory available per block in bytes */
	private int regsPerBlock = 0;
	/** < 32-bit registers available per block */
	private int warpSize = 0;
	/** < Warp size in threads */
	private long memPitch = 0;
	/** < Maximum pitch in bytes allowed by memory copies */
	private int maxThreadsPerBlock = 0;
	/** < Maximum number of threads per block */
	private int[] maxThreadsDim = new int[3];
	/** < Maximum size of each dimension of a block */
	private int[] maxGridSize = new int[3];
	/** < Maximum size of each dimension of a grid */
	private int clockRate = 0;
	/** < Clock frequency in kilohertz */
	private long totalConstMem = 0;
	/** < Constant memory available on device in bytes */
	private int major = 0;
	/** < Major compute capability */
	private int minor = 0;
	/** < Minor compute capability */
	private long textureAlignment = 0;
	/** < Alignment requirement for textures */
	private long texturePitchAlignment = 0;
	/**
	 * < Pitch alignment requirement for texture references bound to pitched
	 * memory
	 */
	private int deviceOverlap = 0;
	/**
	 * < Device can concurrently copy memory and execute a kernel.
	 * Deprecated. Use instead asyncEngineCount.
	 */
	private int multiProcessorCount = 0;
	/** < Number of multiprocessors on device */
	private int kernelExecTimeoutEnabled = 0;
	/** < Specified whether there is a run time limit on kernels */
	private int integrated = 0;
	/** < Device is private integrated as opposed to discrete */
	private int canMapHostMemory = 0;
	/**
	 * < Device can map host memory with
	 * cudaHostAlloc/cudaHostGetDevicePoprivate inter
	 */
	private int computeMode = 0;
	/** < Compute mode (See ::cudaComputeMode) */
	private int maxTexture1D = 0;
	/** < Maximum 1D texture size */
	private int maxTexture1DMipmap = 0;
	/** < Maximum 1D mipmapped texture size */
	private int maxTexture1DLinear = 0;
	/** < Maximum size for 1D textures bound to linear memory */
	private int[] maxTexture2D = new int[2];
	/** < Maximum 2D texture dimensions */
	private int[] maxTexture2DMipmap = new int[2];
	/** < Maximum 2D mipmapped texture dimensions */
	private int[] maxTexture2DLinear = new int[3];
	/**
	 * < Maximum dimensions (width, height, pitch) for 2D textures bound to
	 * pitched memory
	 */
	private int[] maxTexture2DGather = new int[2];
	/**
	 * < Maximum 2D texture dimensions if texture gather operations have to
	 * be performed
	 */
	private int[] maxTexture3D = new int[3];
	/** < Maximum 3D texture dimensions */
	private int[] maxTexture3DAlt = new int[3];
	/** < Maximum alternate 3D texture dimensions */
	private int maxTextureCubemap = 0;
	/** < Maximum Cubemap texture dimensions */
	private int[] maxTexture1DLayered = new int[2];
	/** < Maximum 1D layered texture dimensions */
	private int[] maxTexture2DLayered = new int[3];
	/** < Maximum 2D layered texture dimensions */
	private int[] maxTextureCubemapLayered = new int[2];
	/** < Maximum Cubemap layered texture dimensions */
	private int maxSurface1D = 0;
	/** < Maximum 1D surface size */
	private int[] maxSurface2D = new int[2];
	/** < Maximum 2D surface dimensions */
	private int[] maxSurface3D = new int[3];
	/** < Maximum 3D surface dimensions */
	private int[] maxSurface1DLayered = new int[2];
	/** < Maximum 1D layered surface dimensions */
	private int[] maxSurface2DLayered = new int[3];
	/** < Maximum 2D layered surface dimensions */
	private int maxSurfaceCubemap = 0;
	/** < Maximum Cubemap surface dimensions */
	private int[] maxSurfaceCubemapLayered = new int[2];
	/** < Maximum Cubemap layered surface dimensions */
	private long surfaceAlignment = 0;
	/** < Alignment requirements for surfaces */
	private int concurrentKernels = 0;
	/** < Device can possibly execute multiple kernels concurrently */
	private int ECCEnabled = 0;
	/** < Device has ECC support enabled */
	private int pciBusID = 0;
	/** < PCI bus ID of the device */
	private int pciDeviceID = 0;
	/** < PCI device ID of the device */
	private int pciDomainID = 0;
	/** < PCI domain ID of the device */
	private int tccDriver = 0;
	/** < 1 if device is a Tesla device using TCC driver, 0 otherwise */
	private int asyncEngineCount = 0;
	/** < Number of asynchronous engines */
	private int unifiedAddressing = 0;
	/** < Device shares a unified address space with the host */
	private int memoryClockRate = 0;
	/** < Peak memory clock frequency in kilohertz */
	private int memoryBusWidth = 0;
	/** < Global memory bus width in bits */
	private int l2CacheSize = 0;
	/** < Size of L2 cache in bytes */
	private int maxThreadsPerMultiProcessor = 0;
	/** < Maximum resident threads per multiprocessor */
	private int streamPrioritiesSupported = 0;
	/** < Device supports stream priorities */
	private int globalL1CacheSupported = 0;
	/** < Device supports caching globals in L1 */
	private int localL1CacheSupported = 0;
	/** < Device supports caching locals in L1 */
	private long sharedMemPerMultiprocessor = 0;
	/** < Shared memory available per multiprocessor in bytes */
	private int regsPerMultiprocessor = 0;
	/** < 32-bit registers available per multiprocessor */
	private int managedMemory = 0;
	/** < Device supports allocating managed memory on this system */
	private int isMultiGpuBoard = 0;
	/** < Device is on a multi-GPU board */
	private int multiGpuBoardGroupID = 0;

	/**
	 * < Unique identifier for a group of devices on the same multi-GPU
	 * board
	 */

	public CudaDeviceProp(int runtimeVersion) {
	    this.runtimeVersion=runtimeVersion;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getTotalGlobalMem() {
		return totalGlobalMem;
	}

	public void setTotalGlobalMem(long totalGlobalMem) {
		this.totalGlobalMem = totalGlobalMem;
	}

	public long getSharedMemPerBlock() {
		return sharedMemPerBlock;
	}

	public void setSharedMemPerBlock(long sharedMemPerBlock) {
		this.sharedMemPerBlock = sharedMemPerBlock;
	}

	public int getRegsPerBlock() {
		return regsPerBlock;
	}

	public void setRegsPerBlock(int regsPerBlock) {
		this.regsPerBlock = regsPerBlock;
	}

	public int getWarpSize() {
		return warpSize;
	}

	public void setWarpSize(int warpSize) {
		this.warpSize = warpSize;
	}

	public long getMemPitch() {
		return memPitch;
	}

	public void setMemPitch(long memPitch) {
		this.memPitch = memPitch;
	}

	public int getMaxThreadsPerBlock() {
		return maxThreadsPerBlock;
	}

	public void setMaxThreadsPerBlock(int maxThreadsPerBlock) {
		this.maxThreadsPerBlock = maxThreadsPerBlock;
	}

	public int[] getMaxThreadsDim() {
		return maxThreadsDim;
	}

	public void setMaxThreadsDim(int maxThreadsDim, int index) {
		this.maxThreadsDim[index] = maxThreadsDim;
	}

	public int[] getMaxGridSize() {
		return maxGridSize;
	}

	public void setMaxGridSize(int maxGridSize, int index) {
		this.maxGridSize[index] = maxGridSize;
	}

	public int getClockRate() {
		return clockRate;
	}

	public void setClockRate(int clockRate) {
		this.clockRate = clockRate;
	}

	public long getTotalConstMem() {
		return totalConstMem;
	}

	public void setTotalConstMem(long totalConstMem) {
		this.totalConstMem = totalConstMem;
	}

	public int getMajor() {
		return major;
	}

	public void setMajor(int major) {
		this.major = major;
	}

	public int getMinor() {
		return minor;
	}

	public void setMinor(int minor) {
		this.minor = minor;
	}

	public long getTextureAlignment() {
		return textureAlignment;
	}

	public void setTextureAlignment(long textureAlignment) {
		this.textureAlignment = textureAlignment;
	}

	public long getTexturePitchAlignment() {
		return texturePitchAlignment;
	}

	public void setTexturePitchAlignment(long texturePitchAlignment) {
		this.texturePitchAlignment = texturePitchAlignment;
	}

	public int getDeviceOverlap() {
		return deviceOverlap;
	}

	public void setDeviceOverlap(int deviceOverlap) {
		this.deviceOverlap = deviceOverlap;
	}

	public int getMultiProcessorCount() {
		return multiProcessorCount;
	}

	public void setMultiProcessorCount(int multiProcessorCount) {
		this.multiProcessorCount = multiProcessorCount;
	}

	public int getKernelExecTimeoutEnabled() {
		return kernelExecTimeoutEnabled;
	}

	public void setKernelExecTimeoutEnabled(int kernelExecTimeoutEnabled) {
		this.kernelExecTimeoutEnabled = kernelExecTimeoutEnabled;
	}

	public int getIntegrated() {
		return integrated;
	}

	public void setIntegrated(int integrated) {
		this.integrated = integrated;
	}

	public int getCanMapHostMemory() {
		return canMapHostMemory;
	}

	public void setCanMapHostMemory(int canMapHostMemory) {
		this.canMapHostMemory = canMapHostMemory;
	}

	public int getComputeMode() {
		return computeMode;
	}

	public void setComputeMode(int computeMode) {
		this.computeMode = computeMode;
	}

	public int getMaxTexture1D() {
		return maxTexture1D;
	}

	public void setMaxTexture1D(int maxTexture1D) {
		this.maxTexture1D = maxTexture1D;
	}

	public int getMaxTexture1DMipmap() {
		return maxTexture1DMipmap;
	}

	public void setMaxTexture1DMipmap(int maxTexture1DMipmap) {
		this.maxTexture1DMipmap = maxTexture1DMipmap;
	}

	public int getMaxTexture1DLinear() {
		return maxTexture1DLinear;
	}

	public void setMaxTexture1DLinear(int maxTexture1DLinear) {
		this.maxTexture1DLinear = maxTexture1DLinear;
	}

	public int[] getMaxTexture2D() {
		return maxTexture2D;
	}

	public void setMaxTexture2D(int maxTexture2D, int index) {
		this.maxTexture2D[index] = maxTexture2D;
	}

	public int[] getMaxTexture2DMipmap() {
		return maxTexture2DMipmap;
	}

	public void setMaxTexture2DMipmap(int maxTexture2DMipmap,int index) {
		this.maxTexture2DMipmap[index]= maxTexture2DMipmap;
	}

	public int[] getMaxTexture2DLinear() {
		return maxTexture2DLinear;
	}

	public void setMaxTexture2DLinear(int maxTexture2DLinear, int index) {
		this.maxTexture2DLinear[index] = maxTexture2DLinear;
	}

	public int[] getMaxTexture2DGather() {
		return maxTexture2DGather;
	}

	public void setMaxTexture2DGather(int maxTexture2DGather, int index) {
		this.maxTexture2DGather[index] = maxTexture2DGather;
	}

	public int[] getMaxTexture3D() {
		return maxTexture3D;
	}

	public void setMaxTexture3D(int maxTexture3D, int index) {
		this.maxTexture3D[index] = maxTexture3D;
	}

	public int[] getMaxTexture3DAlt() {
		return maxTexture3DAlt;
	}

	public void setMaxTexture3DAlt(int maxTexture3DAlt, int index) {
		this.maxTexture3DAlt[index] = maxTexture3DAlt;
	}

	public int getMaxTextureCubemap() {
		return maxTextureCubemap;
	}

	public void setMaxTextureCubemap(int maxTextureCubemap) {
		this.maxTextureCubemap = maxTextureCubemap;
	}

	public int[] getMaxTexture1DLayered() {
		return maxTexture1DLayered;
	}

	public void setMaxTexture1DLayered(int maxTexture1DLayered, int index ) {
		this.maxTexture1DLayered[index] = maxTexture1DLayered;
	}

	public int[] getMaxTexture2DLayered() {
		return maxTexture2DLayered;
	}

	public void setMaxTexture2DLayered(int maxTexture2DLayered, int index) {
		this.maxTexture2DLayered[index] = maxTexture2DLayered;
	}

	public int[] getMaxTextureCubemapLayered() {
		return maxTextureCubemapLayered;
	}

	public void setMaxTextureCubemapLayered(int maxTextureCubemapLayered, int index) {
		this.maxTextureCubemapLayered[index] = maxTextureCubemapLayered;
	}

	public int getMaxSurface1D() {
		return maxSurface1D;
	}

	public void setMaxSurface1D(int maxSurface1D) {
		this.maxSurface1D = maxSurface1D;
	}

	public int[] getMaxSurface2D() {
		return maxSurface2D;
	}

	public void setMaxSurface2D(int maxSurface2D, int index) {
		this.maxSurface2D[index] = maxSurface2D;
	}

	public int[] getMaxSurface3D() {
		return maxSurface3D;
	}

	public void setMaxSurface3D(int maxSurface3D, int index) {
		this.maxSurface3D[index] = maxSurface3D;
	}

	public int[] getMaxSurface1DLayered() {
		return maxSurface1DLayered;
	}

	public void setMaxSurface1DLayered(int maxSurface1DLayered, int index) {
		this.maxSurface1DLayered[index] = maxSurface1DLayered;
	}

	public int[] getMaxSurface2DLayered() {
		return maxSurface2DLayered;
	}

	public void setMaxSurface2DLayered(int maxSurface2DLayered, int index) {
		this.maxSurface2DLayered[index] = maxSurface2DLayered;
	}

	public int getMaxSurfaceCubemap() {
		return maxSurfaceCubemap;
	}

	public void setMaxSurfaceCubemap(int maxSurfaceCubemap) {
		this.maxSurfaceCubemap = maxSurfaceCubemap;
	}

	public int[] getMaxSurfaceCubemapLayered() {
		return maxSurfaceCubemapLayered;
	}

	public void setMaxSurfaceCubemapLayered(int maxSurfaceCubemapLayered, int index) {
		this.maxSurfaceCubemapLayered[index] = maxSurfaceCubemapLayered;
	}

	public long getSurfaceAlignment() {
		return surfaceAlignment;
	}

	public void setSurfaceAlignment(long surfaceAlignment) {
		this.surfaceAlignment = surfaceAlignment;
	}

	public int getConcurrentKernels() {
		return concurrentKernels;
	}

	public void setConcurrentKernels(int concurrentKernels) {
		this.concurrentKernels = concurrentKernels;
	}

	public int getECCEnabled() {
		return ECCEnabled;
	}

	public void setECCEnabled(int ECCEnabled) {
		this.ECCEnabled = ECCEnabled;
	}

	public int getPciBusID() {
		return pciBusID;
	}

	public void setPciBusID(int pciBusID) {
		this.pciBusID = pciBusID;
	}

	public int getPciDeviceID() {
		return pciDeviceID;
	}

	public void setPciDeviceID(int pciDeviceID) {
		this.pciDeviceID = pciDeviceID;
	}

	public int getPciDomainID() {
		return pciDomainID;
	}

	public void setPciDomainID(int pciDomainID) {
		this.pciDomainID = pciDomainID;
	}

	public int getTccDriver() {
		return tccDriver;
	}

	public void setTccDriver(int tccDriver) {
		this.tccDriver = tccDriver;
	}

	public int getAsyncEngineCount() {
		return asyncEngineCount;
	}

	public void setAsyncEngineCount(int asyncEngineCount) {
		this.asyncEngineCount = asyncEngineCount;
	}

	public int getUnifiedAddressing() {
		return unifiedAddressing;
	}

	public void setUnifiedAddressing(int unifiedAddressing) {
		this.unifiedAddressing = unifiedAddressing;
	}

	public int getMemoryClockRate() {
		return memoryClockRate;
	}

	public void setMemoryClockRate(int memoryClockRate) {
		this.memoryClockRate = memoryClockRate;
	}

	public int getMemoryBusWidth() {
		return memoryBusWidth;
	}

	public void setMemoryBusWidth(int memoryBusWidth) {
		this.memoryBusWidth = memoryBusWidth;
	}

	public int getL2CacheSize() {
		return l2CacheSize;
	}

	public void setL2CacheSize(int l2CacheSize) {
		this.l2CacheSize = l2CacheSize;
	}

	public int getMaxThreadsPerMultiProcessor() {
		return maxThreadsPerMultiProcessor;
	}

	public void setMaxThreadsPerMultiProcessor(int maxThreadsPerMultiProcessor) {
		this.maxThreadsPerMultiProcessor = maxThreadsPerMultiProcessor;
	}

	public int getStreamPrioritiesSupported() {
		return streamPrioritiesSupported;
	}

	public void setStreamPrioritiesSupported(int streamPrioritiesSupported) {
		this.streamPrioritiesSupported = streamPrioritiesSupported;
	}

	public int getGlobalL1CacheSupported() {
		return globalL1CacheSupported;
	}

	public void setGlobalL1CacheSupported(int globalL1CacheSupported) {
		this.globalL1CacheSupported = globalL1CacheSupported;
	}

	public int getLocalL1CacheSupported() {
		return localL1CacheSupported;
	}

	public void setLocalL1CacheSupported(int localL1CacheSupported) {
		this.localL1CacheSupported = localL1CacheSupported;
	}

	public long getSharedMemPerMultiprocessor() {
		return sharedMemPerMultiprocessor;
	}

	public void setSharedMemPerMultiprocessor(long sharedMemPerMultiprocessor) {
		this.sharedMemPerMultiprocessor = sharedMemPerMultiprocessor;
	}

	public int getRegsPerMultiprocessor() {
		return regsPerMultiprocessor;
	}

	public void setRegsPerMultiprocessor(int regsPerMultiprocessor) {
		this.regsPerMultiprocessor = regsPerMultiprocessor;
	}

	public int getManagedMemory() {
		return managedMemory;
	}

	public void setManagedMemory(int managedMemory) {
		this.managedMemory = managedMemory;
	}

	public int getIsMultiGpuBoard() {
		return isMultiGpuBoard;
	}

	public void setIsMultiGpuBoard(int isMultiGpuBoard) {
		this.isMultiGpuBoard = isMultiGpuBoard;
	}

	public int getMultiGpuBoardGroupID() {
		return multiGpuBoardGroupID;
	}

	public void setMultiGpuBoardGroupID(int multiGpuBoardGroupID) {
		this.multiGpuBoardGroupID = multiGpuBoardGroupID;
	}

	public byte[] getStruct() {
	    // cuda 6.5 -- 632 640 0x78 0x02
		// cuda 8.0 -- 648 650
		// cuda 9.0 -- 672 680
		int size=0;

        if (runtimeVersion>=6000 && runtimeVersion<7000) {
            size = 640;
        } else
        if (runtimeVersion>=7000 && runtimeVersion<8000) {
            size = 640;
        } else
		if (runtimeVersion>=8000 && runtimeVersion<9000) {
		    size=650;
        } else
        if (runtimeVersion >=9000) {
		    size=680;
        }


		int high=size/256;
		int low=size%256;
		byte[] bytes = new byte[size];
		bytes[0] = (byte) low;
		bytes[1] = (byte) high;
		for (int i = 2; i < size; i++) {
			bytes[i] = (byte) 0;
		}
		return bytes;
	}
}
