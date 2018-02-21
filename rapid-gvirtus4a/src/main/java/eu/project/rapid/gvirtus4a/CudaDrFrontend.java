package eu.project.rapid.gvirtus4a;

import android.util.Log;

import java.io.IOException;

import eu.project.rapid.gvirtus4a.params.FloatArrayParam;
import eu.project.rapid.gvirtus4a.params.IntParam;
import eu.project.rapid.gvirtus4a.params.LongParam;
import eu.project.rapid.gvirtus4a.params.StringParam;

public class CudaDrFrontend {

    private static final String LOG_TAG = "CUDA DRIVER FRONTEND";

    private Frontend frontend;

    public CudaDrFrontend(String serverIpAddress, int port) throws IOException {

        frontend=Frontend.getFrontend(serverIpAddress, port);

    }

    public void close() {
        frontend.close();
        frontend=null;

    }

    @Override
    public void finalize() {
        close();
    }
    /*
    public int Execute(String routine) throws IOException {

        int exit_code = frontend.Execute(routine);
        return exit_code;
    }
    */

    /* CUDA DRIVER DEVICE */
    public int cuDeviceGet(int devID, IntParam result) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddPointer(0);
        buffer.AddInt(devID);
        String outputbuffer = "";
        int exit_c = frontend.Execute("cuDeviceGet",buffer);
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

    public int cuDeviceGetName(int len, int dev, StringParam name) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddByte(1);
        for (int i = 0; i < 8; i++)
            buffer.AddByte(0);
        buffer.AddByte(1);
        for (int i = 0; i < 7; i++)
            buffer.AddByte(0);
        buffer.AddInt(len);
        buffer.AddInt(dev);

        String outbuffer = "";
        StringBuilder output = new StringBuilder();
        int exit_c = frontend.Execute("cuDeviceGetName",buffer);
        if (exit_c!=0) { return exit_c; }
        int sizeType = frontend.readByte();
        frontend.readBytes(15);

        for (int i = 0; i < sizeType; i++) {
            byte bit = frontend.readByte();
            outbuffer += Integer.toHexString(bit);
        }
        for (int i = 0; i < outbuffer.length() - 1; i += 2) {
            String str = outbuffer.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));

        }
        name.value=output.toString();
        return exit_c;

    }

    public int cuDeviceGetCount(IntParam result) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddPointer(0);
        String outputbuffer = "";
        int exit_c = frontend.Execute("cuDeviceGetCount",buffer);
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

    public int cuDeviceComputeCapability(int device, IntParam[] results) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddPointer(0);
        buffer.AddPointer(0);
        buffer.AddInt(device);
        String outputbuffer = "";
        int exit_c = frontend.Execute("cuDeviceComputeCapability",buffer);
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


        results[0].value = Integer.valueOf(outputbuffer);
        outputbuffer = "";
        sizeType = frontend.readByte();
        frontend.readBytes(7);

        for (int i = 0; i < sizeType; i++) {
            if (i == 0) {
                byte bb = frontend.readByte();
                outputbuffer += Integer.toHexString(bb & 0xFF);
            } else
                frontend.readByte();
        }
        StringBuilder out3 = new StringBuilder();
        if (outputbuffer.length() > 2) {
            for (int i = 0; i < outputbuffer.length() - 1; i += 2) {
                String str = outputbuffer.substring(i, i + 2);
                out3.insert(0, str);
            }
            outputbuffer = String.valueOf(Integer.parseInt(out3.toString(), 16));
        }
        results[1].value = Integer.valueOf(outputbuffer);

        return exit_c;

    }

    public int cuDeviceGetAttribute(int attribute, int device, IntParam result) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddPointer(0);
        buffer.AddInt(attribute);
        buffer.AddInt(device);
        String outputbuffer = "";
        int exit_c = frontend.Execute("cuDeviceGetAttribute",buffer);
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

    public int cuDeviceTotalMem(int dev, LongParam result) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddByte(8);
        for (int i = 0; i < 16; i++)
            buffer.AddByte(0);
        buffer.AddInt(dev);
        int exit_c = frontend.Execute("cuDeviceTotalMem",buffer);
        if (exit_c!=0) { return exit_c; }
        frontend.readBytes(8);
        result.value = frontend.getLong();
        return exit_c;

    }

	/* CUDA DRIVER MEMORY */

    public int cuMemAlloc(long size, StringParam pointer) throws IOException {
        Buffer buffer=new Buffer();
        byte[] bits = Util.longToByteArray(size);
        for (int i = 0; i < bits.length; i++) {
            buffer.AddByte(bits[i] & 0xFF);
        }

        int exit_c = frontend.Execute("cuMemAlloc",buffer);
        if (exit_c!=0) { return exit_c; }
        pointer.value = frontend.getHex(8);
        return exit_c;
    }

    public int cuMemcpyHtoD(String dst, float[] src, int count) throws IOException {
        Buffer buffer=new Buffer();
        byte[] bits = Util.longToByteArray(count);
        for (int i = 0; i < bits.length; i++) {
            buffer.AddByte(bits[i] & 0xFF);
        }
        buffer.Add(dst);
        for (int i = 0; i < bits.length; i++) {
            buffer.AddByte(bits[i] & 0xFF);
        }

        buffer.Add(src);
        int exit_c = frontend.Execute("cuMemcpyHtoD",buffer);
        if (exit_c!=0) { return exit_c; }
        return exit_c;
    }

    public int cuMemcpyDtoH(String srcDevice, long byteCount, FloatArrayParam result) throws IOException {
        Buffer buffer=new Buffer();
        buffer.Add(srcDevice);

        byte[] bits = Util.longToByteArray(byteCount);
        for (int i = 0; i < bits.length; i++) {
            buffer.AddByte(bits[i] & 0xFF);
        }
        int exit_c = frontend.Execute("cuMemcpyDtoH",buffer);
        if (exit_c!=0) { return exit_c; }

        frontend.readBytes(8);

        int sizeType = (int) byteCount;
        byte[] inBuffer=new byte[sizeType];;

        int bytesToRead=(int)byteCount;
        int bytesRead;

        int offset = 0;
        do {
            bytesRead = frontend.read(inBuffer,offset,bytesToRead);
            bytesToRead=bytesToRead-bytesRead;
            offset=offset+bytesRead;

        } while (offset<byteCount);

        result.values = new float[sizeType/4];
        int i=0;
        for (offset = 0; offset < sizeType; offset += 4) {
            result.values[i] =frontend.getFloat(inBuffer, offset);
            i++;
        }

        return exit_c;


    }

    public int cuMemFree(String ptr) throws IOException {
        Buffer buffer=new Buffer();
        buffer.Add(ptr);
        int exit_c = frontend.Execute("cuMemFree",buffer);
        if (exit_c!=0) { return exit_c; }
        return exit_c;
    }

	/* CUDA DRIVER INITIALIZATION */

    public int cuInit(int flags) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddInt(flags);
        int exit_c = frontend.Execute("cuInit",buffer);
        if (exit_c!=0) { return exit_c; }
        return exit_c;
    }

    /* CUDA DRIVER CONTEXT */
    public int cuCtxCreate(int flags, int dev, StringParam context) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddInt(flags);
        buffer.AddInt(dev);
        int exit_c = frontend.Execute("cuCtxCreate",buffer);
        if (exit_c!=0) { return exit_c; }
        context.value=frontend.getHex(8);
        return exit_c;
    }

    public int cuCtxDestroy(String ctx) throws IOException {
        Buffer buffer=new Buffer();
        buffer.Add(ctx);
        int exit_c = frontend.Execute("cuCtxDestroy",buffer);
        if (exit_c!=0) { return exit_c; }
        return exit_c;
    }

    /* CUDA DRIVER EXECUTION */
    public int cuParamSetv(String hfunc, int offset, String ptr, int numbytes) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddInt(offset);
        buffer.AddInt(numbytes);
        long sizeofp = 8;
        buffer.Add(sizeofp);
        buffer.Add(ptr);
        buffer.Add(hfunc);
        int exit_c = frontend.Execute("cuParamSetv",buffer);
        if (exit_c!=0) { return exit_c; }
        return exit_c;

    }

    public int cuParamSeti(String hfunc, int offset, int value) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddInt(offset);
        buffer.AddInt(value);
        buffer.Add(hfunc);
        int exit_c = frontend.Execute("cuParamSeti",buffer);
        if (exit_c!=0) { return exit_c; }
        return exit_c;
    }

    public int cuParamSetSize(String hfunc, int numbytes) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddInt(numbytes);
        buffer.Add(hfunc);
        int exit_c = frontend.Execute("cuParamSetSize",buffer);
        if (exit_c!=0) { return exit_c; }
        return exit_c;
    }

    public int cuFuncSetBlockShape(String hfunc, int x, int y, int z) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddInt(x);
        buffer.AddInt(y);
        buffer.AddInt(z);
        buffer.Add(hfunc);
        int exit_c = frontend.Execute("cuFuncSetBlockShape",buffer);
        if (exit_c!=0) { return exit_c; }
        return exit_c;
    }

    public int cuFuncSetSharedSize(String hfunc, int bytes) throws IOException {
        Buffer buffer=new Buffer();
        byte[] bits = Util.intToByteArray(bytes);
        for (int i = 0; i < bits.length; i++) {
            buffer.AddByte(bits[i] & 0xFF);
        }
        buffer.Add(hfunc);
        int exit_c = frontend.Execute("cuFuncSetSharedSize",buffer);
        if (exit_c!=0) { return exit_c; }
        return exit_c;
    }

    public int cuLaunchGrid(String hfunc, int grid_width, int grid_height) throws IOException {
        Buffer buffer=new Buffer();
        buffer.AddInt(grid_width);
        buffer.AddInt(grid_height);
        buffer.Add(hfunc);
        int exit_c = frontend.Execute("cuLaunchGrid",buffer);
        if (exit_c!=0) { return exit_c; }
        return exit_c;
    }

	/* CUDA DRIVER MODULE */

    public int cuModuleGetFunction(String cmodule, String str,StringParam pointer) throws IOException {
        Buffer buffer=new Buffer();

        str = str + "\0";
        long size = str.length();
        byte[] bits = Util.longToByteArray(size);

        for (int i = 0; i < bits.length; i++) {
            buffer.AddByte(bits[i] & 0xFF);
        }
        for (int i = 0; i < bits.length; i++) {
            buffer.AddByte(bits[i] & 0xFF);
        }
        for (int i = 0; i < size; i++) {
            buffer.AddByte(str.charAt(i));
        }

        buffer.Add(cmodule);

        IntParam result=new IntParam();
        int exit_c = frontend.Execute("cuModuleGetFunction",buffer,result);
        if (exit_c!=0) { return exit_c; }
        int resultBufferSize=result.value;
        pointer.value = frontend.getHex(8);
        for (int i = 0; i <resultBufferSize - 8; i++) {
            frontend.readByte();
        }

        return exit_c;

    }

    public int cuModuleLoadDataEx(String ptxSource, int jitNumOptions,
                                     int[] jitOptions, long jitOptVals0, char[] jitOptVals1, long jitOptVals2, StringParam pointer) throws IOException {

        Log.v(LOG_TAG, "Entered cuModuleLoadDataEx");
        Log.v(LOG_TAG, "PTXsource length: " + ptxSource.length());

        Buffer buffer=new Buffer();

        buffer.AddInt(jitNumOptions);
        buffer.Add(jitOptions);

        Log.v(LOG_TAG, "cuModuleLoadDataEx 1");

        // addStringForArgument
        ptxSource = ptxSource + "\0";
        long sizePtxSource = ptxSource.length();

        Log.v(LOG_TAG, "cuModuleLoadDataEx 2");

        long size = sizePtxSource;
        byte[] bits = Util.longToByteArray(size);

        Log.v(LOG_TAG, "cuModuleLoadDataEx 3");

        for (int i = 0; i < bits.length; i++) {
            buffer.AddByte(bits[i] & 0xFF);
        }

        Log.v(LOG_TAG, "cuModuleLoadDataEx 4");

        for (int i = 0; i < bits.length; i++) {
            buffer.AddByte(bits[i] & 0xFF);
        }

        Log.v(LOG_TAG, "cuModuleLoadDataEx 5");

        buffer.AddByte4Ptx(ptxSource, sizePtxSource);

        Log.v(LOG_TAG, "cuModuleLoadDataEx 6");

        buffer.Add(8);
        long OptVals0 = jitOptVals0;
        byte[] bit = Util.longToByteArray(OptVals0);
        for (int i = 0; i < bit.length; i++) {
            buffer.AddByte(bit[i] & 0xFF);
        }

        Log.v(LOG_TAG, "cuModuleLoadDataEx 7");

        buffer.Add(8);
        buffer.AddByte(160);
        buffer.AddByte(159);
        buffer.AddByte(236);
        buffer.AddByte(1);
        buffer.AddByte(0);
        buffer.AddByte(0);
        buffer.AddByte(0);
        buffer.AddByte(0);

        Log.v(LOG_TAG, "cuModuleLoadDataEx 8");

        buffer.Add(8);
        long OptVals2 = jitOptVals2;
        byte[] bit2 = Util.longToByteArray(OptVals2);
        for (int i = 0; i < bit.length; i++) {
            buffer.AddByte(bit2[i] & 0xFF);
        }

        Log.v(LOG_TAG, "cuModuleLoadDataEx 9");

        IntParam result=new IntParam();
        int exit_c = frontend.Execute("cuModuleLoadDataEx",buffer,result);
        if (exit_c!=0) { return exit_c; }
        int resultBufferSize=result.value;

        Log.v(LOG_TAG, "cuModuleLoadDataEx 10");

        pointer.value = frontend.getHex(8);

        Log.v(LOG_TAG, "cuModuleLoadDataEx 11");

        for (int i = 0; i < resultBufferSize - 8; i++)
            frontend.readByte();

        return exit_c;
    }

}
