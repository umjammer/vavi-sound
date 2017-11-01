package vavi.sound.adpcm.dvi;

public class Convert {

    public static void int2byte(int ival, byte b[], int offset) {
        int i;
        int bits = 32;

        for(i = 0; i < 4; i++) {
            bits -= 8;
            b[offset + i] = (byte) ((ival >> bits) & 0xff);
        }
    }

    public static int byte2int(byte b[], int offset) {
        int i;
        int val = 0;
        int bits = 32;
        int tval;

        for(i = 0; i < 4; i++) {
            bits -= 8;
            tval = b[offset + i];
            tval = tval < 0 ? 256 + tval : tval;
            val |= tval << bits;
        }

        return val;
    }

    public static void short2byte(short ival, byte b[], int offset) {
        int i;
        int bits = 16;

        for(i = 0; i < 2; i++) {
            bits -= 8;
            b[offset + i] = (byte) ((ival >> bits) & 0xff);
        }
    }

    public static short byte2short(byte b[], int offset) {
        int i;
        short val = 0;
        int bits = 16;
        int tval;

        for(i = 0; i < 2; i++) {
            bits -= 8;
            tval = b[offset + i];
            tval = tval < 0 ? 256 + tval : tval;
            val |= tval << bits;
        }

        return val;
    }

    public static void long2byte(long ival, byte b[], int offset) {
        int i;
        int bits = 64;

        for(i = 0; i < 8; i++) {
            bits -= 8;
            b[offset + i] = (byte) ((ival >> bits) & 0xff);
        }
    }

    public static long byte2long(byte b[], int offset) {
        int i;
        long val = 0;
        int bits = 64;
        long tval;

        for(i = 0; i < 8; i++) {
            bits -= 8;
            tval = b[offset + i];
            tval = tval < 0 ? 256 + tval : tval;
            val |= tval << bits;
        }

        return val;
    }
}

