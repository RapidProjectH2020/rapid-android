package eu.project.rapid.gvirtus4a;

import java.io.IOException;
import java.util.Vector;

import eu.project.rapid.gvirtus4a.params.FloatArrayParam;
import eu.project.rapid.gvirtus4a.params.IntParam;
import eu.project.rapid.gvirtus4a.params.StringParam;

/**
 * Created by raffaelemontella on 26/04/2017.
 */

public class Provider {
    private static final String LOG_TAG = "PROVIDER";
    private String host;
    private int port;
    private Integer driverVersion;
    private Integer runtimeVersion;
    private long wallClock;

    public long getWallClock() { return wallClock; }

    private Vector<CudaDeviceProp> properties=new Vector<CudaDeviceProp>();

    public Provider(String host, int port) {
        this.host=host;
        this.port=port;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public int getDriverVersion() { return driverVersion; }
    public int getRuntimeVersion() { return runtimeVersion; }
    public int getDeviceCount() { return properties.size(); }
    public CudaDeviceProp getDeviceProperties(int index) { return properties.get(index); }
    public CudaDeviceProp getDeviceProperties() { return properties.get(0); }

    public void deviceQuery() throws IOException {

        CudaRtFrontend cudaRtFrontend = new CudaRtFrontend(host, port);
        IntParam intParam=new IntParam();
        int exitCode = cudaRtFrontend.cudaGetDeviceCount(intParam);
        if (exitCode != 0) {
            return;
        }
        int deviceCount=intParam.value;

        cudaRtFrontend.cudaDriverGetVersion(intParam);
        driverVersion=intParam.value;

        cudaRtFrontend.cudaRuntimeGetVersion(intParam);
        runtimeVersion=intParam.value;

        for (int i = 0; i < deviceCount; i++) {
            cudaRtFrontend.cudaSetDevice(i);
            CudaDeviceProp deviceProp=new CudaDeviceProp(runtimeVersion);
            exitCode=cudaRtFrontend.cudaGetDeviceProperties(i,deviceProp);
            if (exitCode==0) {
                properties.add(deviceProp);
            }
        }
        cudaRtFrontend.close();
    }

    public boolean matrixMul() throws IOException {
        long t0=System.currentTimeMillis();
        int widthA=8, heightA=12, widthB=8;

        final float valB = 0.01f;
        int exit_c=0;

        CudaDrFrontend driver = new CudaDrFrontend(host,port);


        exit_c=driver.cuInit(0);
        if (exit_c != 0) {
            throw new RuntimeException(new CudaException(exit_c));
        }
        StringParam spCuContext = new StringParam();

        exit_c=driver.cuCtxCreate(0, 0, spCuContext);
        if (exit_c != 0) {
            throw new RuntimeException(new CudaException(exit_c));
        }
        String cuContext = spCuContext.value;

        IntParam ipDevice = new IntParam();
        exit_c=driver.cuDeviceGet(0, ipDevice);
        if (exit_c != 0) {
            throw new RuntimeException(new CudaException(exit_c));
        }



        int jitNumOptions = 3;
        int[] jitOptions = new int[jitNumOptions];

        // set up size of compilation log buffer
        jitOptions[0] = 4;// CU_JIT_INFO_LOG_BUFFER_SIZE_BYTES;
        long jitLogBufferSize = 1024;
        long jitOptVals0 = jitLogBufferSize;

        // set up pointer to the compilation log buffer
        jitOptions[1] = 3;// CU_JIT_INFO_LOG_BUFFER;

        char[] jitLogBuffer = new char[(int) jitLogBufferSize];
        char[] jitOptVals1 = jitLogBuffer;



        // set up pointer to set the Maximum # of registers for a particular
        // kernel
        jitOptions[2] = 0;// CU_JIT_MAX_REGISTERS;
        long jitRegCount = 32;
        long jitOptVals2 = jitRegCount;



        StringParam spModule = new StringParam();
        exit_c=driver.cuModuleLoadDataEx(
                MatrixMulKernel64.getPtxSource(), jitNumOptions, jitOptions, jitOptVals0,
                jitOptVals1, jitOptVals2, spModule);
        if (exit_c != 0) {
            throw new RuntimeException(new CudaException(exit_c));
        }
        String cmodule = spModule.value;


        StringParam spFunction = new StringParam();
        exit_c=driver.cuModuleGetFunction(cmodule, "matrixMul_bs32_32bit", spFunction);
        if (exit_c != 0) {
            throw new RuntimeException(new CudaException(exit_c));
        }
        String cfunction = spFunction.value;


        // allocate host memory for matrices A and B
        int block_size = 32; // larger block size is for Fermi and above
        final int WA = (widthA * block_size); // Matrix A width
        final int HA = (heightA * block_size); // Matrix A height
        final int WB = (widthB * block_size); // Matrix B width
        final int HB = WA; // Matrix B height
        int WC = WB; // Matrix C width
        int HC = HA; // Matrix C height

        int size_A = WA * HA;
        int mem_size_A = Float.SIZE / 8 * size_A;
        float[] h_A = new float[size_A];
        int size_B = WB * HB;
        int mem_size_B = Float.SIZE / 8 * size_B;
        float[] h_B = new float[size_B];


        h_A = constantInit(h_A, size_A, 1.0f);
        h_B = constantInit(h_B, size_B, valB);
        // allocate device memory

        StringParam spD_A = new StringParam();
        exit_c=driver.cuMemAlloc(mem_size_A, spD_A);
        if (exit_c != 0) {
            throw new RuntimeException(new CudaException(exit_c));
        }
        String d_A = spD_A.value;

        StringParam spD_B = new StringParam();
        exit_c=driver.cuMemAlloc(mem_size_B, spD_B);
        if (exit_c != 0) {
            throw new RuntimeException(new CudaException(exit_c));
        }
        String d_B = spD_B.value;

        driver.cuMemcpyHtoD(d_A, h_A, mem_size_A);
        driver.cuMemcpyHtoD(d_B, h_B, mem_size_B);
        // allocate device memory for result
        long size_C = WC * HC;
        float[] h_C;



        long mem_size_C = Float.SIZE / 8 * size_C;

        StringParam spD_C = new StringParam();
        exit_c=driver.cuMemAlloc(mem_size_C, spD_C);
        if (exit_c != 0) {
            throw new RuntimeException(new CudaException(exit_c));
        }
        String d_C = spD_C.value;

        Util.Dim3 grid = new Util.Dim3(WC / block_size, HC / block_size, 1);

        int offset = 0;
        // setup execution parameters



        driver.cuParamSetv(cfunction, offset, d_C, Util.Sizeof.LONG);

        offset += Util.Sizeof.LONG;
        driver.cuParamSetv(cfunction, offset, d_A, Util.Sizeof.LONG);
        offset += Util.Sizeof.LONG;
        driver.cuParamSetv(cfunction, offset, d_B, Util.Sizeof.LONG);
        offset += Util.Sizeof.LONG;



        int Matrix_Width_A = WA;
        int Matrix_Width_B = WB;
        int Sizeof_Matrix_Width_A = Util.Sizeof.INT;
        int Sizeof_Matrix_Width_B = Util.Sizeof.INT;


        driver.cuParamSeti(cfunction, offset, Matrix_Width_A);



        offset += Sizeof_Matrix_Width_A;
        driver.cuParamSeti(cfunction, offset, Matrix_Width_B);
        offset += Sizeof_Matrix_Width_B;



        driver.cuParamSetSize(cfunction, offset);
        driver.cuFuncSetBlockShape(cfunction, block_size, block_size, grid.z);
        driver.cuFuncSetSharedSize(cfunction, 2 * block_size * block_size * (Float.SIZE / 8));
        driver.cuLaunchGrid(cfunction, grid.x, grid.y);

        FloatArrayParam fapH_C = new FloatArrayParam();
        exit_c=driver.cuMemcpyDtoH(d_C, mem_size_C, fapH_C);
        if (exit_c != 0) {
            throw new RuntimeException(new CudaException(exit_c));
        }
        h_C = fapH_C.values;



        boolean correct = true;
        for (int i = 0; i < WC * HC; i++) {
            if (Math.abs(h_C[i] - (WA * valB)) > 1e-2) {
                correct = false;
            }
        }



        driver.cuMemFree(d_A);
        driver.cuMemFree(d_B);
        driver.cuMemFree(d_C);
        driver.cuCtxDestroy(cuContext);

        driver.close();
        wallClock=System.currentTimeMillis()-t0;
        return correct;
    }

    public static float[][] makeMatrix(int dim1, int dim2, float valB) {
        float[][] matrix = new float[dim1][dim2];
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix[i].length; j++)
                matrix[i][j] = valB;
        return matrix;
    }

    public static float[] constantInit(float[] data, int size, float val) {
        for (int i = 0; i < size; ++i) {
            data[i] = val;
        }
        return data;
    }
}
