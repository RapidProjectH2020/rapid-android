package eu.project.rapid.gvirtus4a;


import android.util.Log;

public final class Buffer {

    public final static String LOG_TAG = "BUFFER";

    static {
        try {
            System.loadLibrary("native-lib"); // Load native library at runtime
        } catch (UnsatisfiedLinkError e) {
            Log.e(LOG_TAG, "Could not load native library, maybe this is running on the VM.");
        }
    }

    private String mpBuffer = "";

    public Buffer() {
        mpBuffer = "";
    }

    public void clear() {
        mpBuffer = "";
    }

    public void AddPointerNull() {
        byte[] bites = {(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        mpBuffer += Util.bytesToHex(bites);
    }

    public void Add(int item) {
        byte[] bites = {(byte) item, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        mpBuffer += Util.bytesToHex(bites);
    }

    public void Add(long item) {
        byte[] bites = {(byte) item, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
        mpBuffer += Util.bytesToHex(bites);
    }

    public void Add(String item) {
        byte[] bites = Util.hexToBytes(item);
        mpBuffer += Util.bytesToHex(bites);
    }

    public void Add(float[] item) {
        String js = prepareFloat(item);  // invoke the native method
        mpBuffer += js;

    }

    public void Add(int[] item) {
        Add(item.length * 4);
        for (int i = 0; i < item.length; i++) {
            AddInt(item[i]);
        }
    }

    public void AddInt(int item) {
        byte[] bits = Util.intToByteArray(item);
        mpBuffer += Util.bytesToHex(bits);

    }

    public void AddPointer(int item) {
        byte[] bites = {(byte) item, (byte) 0, (byte) 0, (byte) 0};
        int size = (Util.Sizeof.INT);
        Add(size);
        mpBuffer += Util.bytesToHex(bites);
    }

    public String GetString() {
        return mpBuffer;
    }

    public long Size() {
        return mpBuffer.length();
    }

    public void AddStruct(byte[] bytes) {

        mpBuffer += Util.bytesToHex(bytes);
    }


    public void AddByte(int i) {
        String jps = prepareSingleByte(i);  // invoke the native method
        mpBuffer += jps;
    }


    public void AddByte4Ptx(String ptxSource, long size) {
        String jps = preparePtxSource(ptxSource, size);  // invoke the native method
        mpBuffer += jps;
    }

    public void printMpBuffer() {
        System.out.println("mpBUFFER : " + mpBuffer);
    }

    public static native String prepareFloat(float[] floats);

    public static native String preparePtxSource(String ptxSource, long size);

    public static native String prepareSingleByte(int i);

}
