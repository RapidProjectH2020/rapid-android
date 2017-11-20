package eu.project.rapid.gvirtus4a;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by raffaelemontella on 18/11/2017.
 */

public class Transmitter {
    private static final String LOG_TAG = "TRANSMITTER";
    private DataOutputStream dos;
    private DataInputStream dis;
    public Transmitter(DataInputStream dis, DataOutputStream dataOutputStream) {
        this.dis=dis;
        this.dos=dos;
    }

    public void writeLong(long l) throws IOException {
        dos.write((byte) l);
        dos.write((byte) (l >> 56));
        dos.write((byte) (l >> 48));
        dos.write((byte) (l >> 40));
        dos.write((byte) (l >> 32));
        dos.write((byte) (l >> 24));
        dos.write((byte) (l >> 16));
        dos.write((byte) (l >> 8));
    }

    public void writeChar( char l) throws IOException {
        dos.write((byte) l);
        dos.write((byte) (l >> 56));
        dos.write((byte) (l >> 48));
        dos.write((byte) (l >> 40));
        dos.write((byte) (l >> 32));
        dos.write((byte) (l >> 24));
        dos.write((byte) (l >> 16));
        dos.write((byte) (l >> 8));
    }

    public void writeInt(int l) throws IOException {
        dos.write((byte) l);
        dos.write((byte) (l >> 24));
        dos.write((byte) (l >> 16));
        dos.write((byte) (l >> 8));
    }

    public void writeHex(long x) throws IOException {
        String hex = Integer.toHexString((int) (x));
        StringBuilder out2 = new StringBuilder();
        int scarto = 0;
        if (hex.length() > 2) {
            for (int i = hex.length() - 1; i > 0; i -= 2) {
                String str = hex.substring(i - 1, i + 1);
                out2.insert(0, str);
                dos.write((byte) Integer.parseInt(out2.toString(), 16));
                scarto += 2;
            }
            if (scarto != hex.length()) {
                dos.write((byte) Integer.parseInt(hex.substring(0, 1), 16));
            }
        }
        dos.write((byte) (0));
        dos.write((byte) (0));
        dos.write((byte) (0));
        dos.write((byte) (0));
        dos.write((byte) (0));
        dos.write((byte) (0));
    }

    public char readChar(DataInputStream dis) throws IOException {
        int x;
        x = dis.readByte();
        x = x >> 56;
        x = dis.readByte();
        x = x >> 48;
        x = dis.readByte();
        x = x >> 40;
        x = dis.readByte();
        x = x >> 32;
        x = dis.readByte();
        x = x >> 24;
        x = dis.readByte();
        x = x >> 16;
        x = dis.readByte();
        x = x >> 8;
        x = dis.readByte();
        return (char) x;

    }

    public String getHex(int size) throws IOException {
        byte[] array = new byte[size];
        for (int i = 0; i < size; i++) {
            byte bit = dis.readByte();
            array[i] = bit;
        }
        return Util.bytesToHex(array);
    }

    public int getInt() throws IOException {

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            byte bit = dis.readByte();
            int a = bit & 0xFF;
            if (a == 0) {
                output.insert(0, Integer.toHexString(a));
                output.insert(0, Integer.toHexString(a));
            } else {
                output.insert(0, Integer.toHexString(a));
            }
        }
        return Integer.parseInt(output.toString(), 16);

    }

    public long getLong() throws IOException {

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            byte bit = dis.readByte();
            int a = bit & 0xFF;
            if (a == 0) {
                output.insert(0, Integer.toHexString(a));
                output.insert(0, Integer.toHexString(a));
            } else {
                output.insert(0, Integer.toHexString(a));
            }
        }
        return Long.parseLong(output.toString(), 16);
    }

    public float getFloat() throws IOException {
        byte[] inBuffer = new byte[4];
        dis.read(inBuffer, 0, 4);
        return getFloat(inBuffer, 0);
    }

    public float getFloat(byte[] inBuffer, int offset) throws IOException {
        String output = Util.bytesToHex(new byte[]{inBuffer[offset + 3], inBuffer[offset + 2], inBuffer[offset + 1], inBuffer[offset]});
        Long i = Long.parseLong(output, 16);
        Float f = Float.intBitsToFloat(i.intValue());
        return f;
    }

}
