package eu.project.rapid.gvirtus4a;

/*
 * Create the .h for native functions:
 * javah Buffer$Helper
 * 
 * Compile the c files with:
 * gcc -I/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.101.x86_64/include/ 
 * -I/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.101.x86_64/include/linux/ -o libndkBuffer.so -shared -fPIC Buffer.c
 * 
 * Execute the java files with this VM argument:
 * -Djava.library.path=.
 */


import android.os.StrictMode;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import eu.project.rapid.gvirtus4a.params.IntParam;

public final class Frontend {

    private static final String LOG_TAG = "FRONTEND";

    private static String serverIpAddress;
    private static int port;
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;

    private Transmitter transmitter;

    private Frontend(String serverIpAddress, int port) throws IOException{
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Frontend.serverIpAddress = serverIpAddress;
        Frontend.port = port;
        //try {
            Log.i(LOG_TAG, "Connecting to GPU backend " + Frontend.serverIpAddress + ":" + Frontend.port);
            socket = new Socket();
            socket.connect(new InetSocketAddress(serverIpAddress, port), 1000);
            //socket = new Socket(serverIpAddress, port);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            transmitter=new Transmitter(dis,dos);
        //} catch (IOException ex) {
        //    // TODO gestire la mancata connessione
        //    throw new RuntimeException(ex);
        //
        //}

    }

    public void close() {
        try {
            if (dis!=null) dis.close();
            if (dos!=null) dos.close();
            if (socket!=null) socket.close();
            dis=null;
            dos=null;
            socket=null;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    @Override
    public void finalize() {
        close();
    }

    public Integer getInt() throws IOException {
        return transmitter.getInt();
    }

    public Long getLong() throws IOException {
        return transmitter.getLong();
    }

    public Float getFloat(byte[] buffer, int offeset) throws IOException {
        return transmitter.getFloat(buffer,offeset);
    }

    public String getHex(int size) throws IOException{
        return transmitter.getHex(size);
    }

    public static Frontend getFrontend(String serverIpAddress, int port) throws IOException {
        Frontend.serverIpAddress=serverIpAddress;
        Frontend.port=port;

        return new Frontend(serverIpAddress, port);
    }

    public static Frontend getFrontend() throws IOException{
        Frontend.serverIpAddress=serverIpAddress;
        Frontend.port=port;

        return new Frontend(serverIpAddress, port);
    }

    public  int Execute(String routine, Buffer buffer) throws IOException {
        return Execute(routine,buffer,null);
    }

    public  int Execute(String routine, Buffer buffer, IntParam result) throws IOException {

        Log.v(LOG_TAG, "Entered Execute() - " + buffer.GetString());

        long size = buffer.Size() / 2;
        byte[] bits = Util.longToByteArray(size);

        byte[] bytes2 = Util.hexToBytes(buffer.GetString());

        Log.v(LOG_TAG, "Execute 1");

        byte[] outBuffer = new byte[routine.length() + 1 + bits.length + bytes2.length];

        int j = 0;
        for (int i = 0; i < routine.length(); i++) {
            outBuffer[j] = (byte) routine.charAt(i);
            j++;
        }
        outBuffer[j] = 0;
        j++;
        for (int i = 0; i < bits.length; i++) {
            outBuffer[j] = (byte) (bits[i] & 0xFF);
            j++;
        }

        for (int i = 0; i < bytes2.length; i++) {
            outBuffer[j] = (byte) (bytes2[i] & 0xFF);
            j++;
        }

        Log.v(LOG_TAG, "Execute 2");

        dos.write(outBuffer);

        Log.v(LOG_TAG, "Execute 3");

        /**************/

		/*
         //System.out.println("Routine called: " + routine);
		for (int i = 0; i < routine.length(); i++)
			outputStream.writeByte(routine.charAt(i));
		outputStream.writeByte(0);
		long size = Buffer.Size() / 2;
		byte[] bits = Util.longToByteArray(size);

		for (int i = 0; i < bits.length; i++) {
			outputStream.write(bits[i] & 0xFF);
		}

		byte[] bytes2 = Util.hexToBytes(Buffer.GetString());
		for (int i = 0; i < bytes2.length; i++) {
			outputStream.write(bytes2[i] & 0xFF);
		}


		 int message = in.readByte(); // use this for exitcode of single routine
		//in.readByte();
		in.readByte();
		in.readByte();
		in.readByte();

		 = (int) in.readByte();
		for (int i = 0; i < 7; i++)
			in.readByte();
		*/
		int expected = 12;
        int totalRead = 0;
        int read = 0;
        byte[] inBuffer = new byte[expected];
        while (totalRead < expected) {
            Log.v(LOG_TAG, "Execute 3.1, number of bytes read: " + totalRead + ", expected: " + expected);
            read = dis.read(inBuffer, totalRead, expected - totalRead);
            totalRead += read;
            Log.v(LOG_TAG, "Execute 3.2, number of bytes read: " + totalRead + ", expected: " + expected);
        }
//        in.read(inBuffer, 0, 12);

        Log.v(LOG_TAG, "Execute 4");

        if (result!=null) {
            result.value = inBuffer[4];
        }
        return inBuffer[0];

    }

    public byte readByte() throws IOException {
        return dis.readByte();
    }

    public byte[] readBytes(int n) throws IOException{
        return readBytes(0,n);
    }

    public byte[] readBytes(int offset, int n) throws IOException{
        byte[] buffer=new byte[n];
        dis.read(buffer,offset,n);
        return buffer;
    }

    public int read(byte[] buffer, int offset, int n) throws IOException {
        return dis.read(buffer,offset,n);
    }

    public int flush() {
        int count=0;
        try {
            while (dis.available() > 0) {
                readByte();
                count++;
            }
        } catch (IOException ex) {
            // TODO gestire la mancata connessione
            throw new RuntimeException(ex);

        }
        return count;
    }
}
