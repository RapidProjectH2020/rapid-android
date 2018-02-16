package eu.project.rapid.gvirtus4a;

import java.util.HashMap;
import java.util.Vector;

/**
 * Created by raffaelemontella on 16/02/2018.
 */

public class CudaException extends Exception {
    private static HashMap<Integer,CudaError> messages;

    /*
    From CUDA Toolkit Documentation
    http://docs.nvidia.com/cuda/cuda-runtime-api/group__CUDART__TYPES.html#group__CUDART__TYPES_1g3f51e3575c2178246db0a94a430e0038
     */
    static {
        messages=new HashMap<>();
        messages.put(0,new CudaError(0,"cudaSuccess","The API call returned with no errors. In the case of query calls, this can also mean that the operation being queried is complete."));
        messages.put(1,new CudaError(1,"cudaErrorMissingConfiguration",""));
        messages.put(1,new CudaError(2,"cudaErrorMemoryAllocation",""));
        messages.put(1,new CudaError(3,"cudaErrorInitializationError",""));
        messages.put(1,new CudaError(4,"cudaErrorLaunchFailure",""));
        messages.put(1,new CudaError(5,"cudaErrorPriorLaunchFailure",""));
        messages.put(1,new CudaError(6,"cudaErrorLaunchTimeout",""));
        messages.put(1,new CudaError(7,"cudaErrorLaunchOutOfResources",""));
        messages.put(1,new CudaError(8,"cudaErrorInvalidDeviceFunction",""));
        messages.put(1,new CudaError(9,"cudaErrorInvalidConfiguration",""));
        messages.put(1,new CudaError(10,"cudaErrorInvalidDevice",""));
        messages.put(1,new CudaError(11,"cudaErrorInvalidValue",""));
        messages.put(1,new CudaError(12,"cudaErrorInvalidPitchValue",""));
        messages.put(1,new CudaError(13,"cudaErrorInvalidSymbol",""));
        messages.put(1,new CudaError(14,"cudaErrorMapBufferObjectFailed",""));
        messages.put(1,new CudaError(15,"cudaErrorUnmapBufferObjectFailed",""));
        messages.put(1,new CudaError(16,"cudaErrorInvalidHostPointer",""));
        messages.put(1,new CudaError(17,"cudaErrorInvalidDevicePointer",""));
        messages.put(1,new CudaError(18,"cudaErrorInvalidTexture",""));
        messages.put(1,new CudaError(19,"cudaErrorInvalidTextureBinding",""));
        messages.put(1,new CudaError(20,"cudaErrorInvalidChannelDescriptor",""));
        messages.put(1,new CudaError(21,"cudaErrorInvalidMemcpyDirection",""));
        messages.put(1,new CudaError(22,"cudaErrorAddressOfConstant",""));
        messages.put(1,new CudaError(23,"cudaErrorTextureFetchFailed",""));
        messages.put(1,new CudaError(24,"cudaErrorTextureNotBound",""));
        messages.put(1,new CudaError(25,"cudaErrorSynchronizationError",""));
        messages.put(1,new CudaError(26,"cudaErrorInvalidFilterSetting",""));
        messages.put(1,new CudaError(27,"cudaErrorInvalidNormSetting",""));
        messages.put(1,new CudaError(28,"cudaErrorMixedDeviceExecution",""));
        messages.put(1,new CudaError(29,"cudaErrorCudartUnloading",""));
        messages.put(1,new CudaError(30,"cudaErrorUnknown",""));
        messages.put(1,new CudaError(31,"cudaErrorNotYetImplemented",""));
        messages.put(1,new CudaError(32,"cudaErrorMemoryValueTooLarge",""));
        messages.put(1,new CudaError(33,"cudaErrorInvalidResourceHandle",""));
        messages.put(1,new CudaError(34,"cudaErrorNotReady",""));
        messages.put(1,new CudaError(35,"cudaErrorInsufficientDriver",""));
        messages.put(1,new CudaError(36,"cudaErrorSetOnActiveProcess",""));
        messages.put(1,new CudaError(37,"cudaErrorInvalidSurface",""));
        messages.put(1,new CudaError(38,"cudaErrorNoDevice",""));
        messages.put(1,new CudaError(39,"cudaErrorECCUncorrectable",""));
        messages.put(1,new CudaError(40,"cudaErrorSharedObjectSymbolNotFound",""));
        messages.put(1,new CudaError(41,"cudaErrorSharedObjectInitFailed",""));
        messages.put(1,new CudaError(42,"cudaErrorUnsupportedLimit",""));
        messages.put(1,new CudaError(43,"cudaErrorDuplicateVariableName",""));
        messages.put(1,new CudaError(44,"cudaErrorDuplicateTextureName",""));
        messages.put(1,new CudaError(45,"cudaErrorDuplicateSurfaceName",""));
        messages.put(1,new CudaError(46,"cudaErrorDevicesUnavailable",""));
        messages.put(1,new CudaError(47,"cudaErrorInvalidKernelImage",""));
        messages.put(1,new CudaError(48,"cudaErrorNoKernelImageForDevice",""));
        messages.put(1,new CudaError(49,"cudaErrorIncompatibleDriverContext",""));
        messages.put(1,new CudaError(50,"cudaErrorPeerAccessAlreadyEnabled",""));
        messages.put(1,new CudaError(51,"cudaErrorPeerAccessNotEnabled",""));
        messages.put(1,new CudaError(54,"cudaErrorDeviceAlreadyInUse",""));
        messages.put(1,new CudaError(55,"cudaErrorProfilerDisabled",""));
        messages.put(1,new CudaError(56,"cudaErrorProfilerNotInitialized",""));
        messages.put(1,new CudaError(57,"cudaErrorProfilerAlreadyStarted",""));
        messages.put(1,new CudaError(58,"cudaErrorProfilerAlreadyStopped",""));
        messages.put(1,new CudaError(59,"cudaErrorAssert",""));
        messages.put(1,new CudaError(60,"cudaErrorTooManyPeers",""));
        messages.put(1,new CudaError(61,"cudaErrorHostMemoryAlreadyRegistered",""));
        messages.put(1,new CudaError(62,"cudaErrorHostMemoryNotRegistered",""));
        messages.put(1,new CudaError(63,"cudaErrorOperatingSystem",""));
        messages.put(1,new CudaError(64,"cudaErrorPeerAccessUnsupported",""));
        messages.put(1,new CudaError(65,"cudaErrorLaunchMaxDepthExceeded",""));
        messages.put(1,new CudaError(66,"cudaErrorLaunchFileScopedTex",""));
        messages.put(1,new CudaError(67,"cudaErrorLaunchFileScopedSurf",""));
        messages.put(1,new CudaError(68,"cudaErrorSyncDepthExceeded",""));
        messages.put(1,new CudaError(69,"cudaErrorLaunchPendingCountExceeded",""));
        messages.put(1,new CudaError(70,"cudaErrorNotPermitted",""));
        messages.put(1,new CudaError(71,"cudaErrorNotSupported",""));
        messages.put(1,new CudaError(72,"cudaErrorHardwareStackError",""));
        messages.put(1,new CudaError(73,"cudaErrorIllegalInstruction",""));
        messages.put(1,new CudaError(74,"cudaErrorMisalignedAddress",""));
        messages.put(1,new CudaError(75,"cudaErrorInvalidAddressSpace",""));
        messages.put(1,new CudaError(76,"cudaErrorInvalidPc",""));
        messages.put(1,new CudaError(77,"cudaErrorIllegalAddress",""));
        messages.put(1,new CudaError(78,"cudaErrorInvalidPtx",""));
        messages.put(1,new CudaError(79,"cudaErrorInvalidGraphicsContext",""));
        messages.put(1,new CudaError(80,"cudaErrorNvlinkUncorrectable",""));
        messages.put(1,new CudaError(81,"cudaErrorJitCompilerNotFound",""));
        messages.put(1,new CudaError(82,"cudaErrorCooperativeLaunchTooLarge",""));
        messages.put(1,new CudaError(0x7f,"cudaErrorStartupFailure","This indicates an internal startup failure in the CUDA runtime."));
        messages.put(1,new CudaError(10000,"cudaErrorApiFailureBase","Any unhandled CUDA driver error is added to this value and returned via the runtime. Production releases of CUDA should not return such errors."));

    }

    private CudaError cudaError;

    public CudaException(int exit_c) {
        super(messages.get(exit_c).getLabel());
        this.cudaError=messages.get(exit_c);
    }
}
