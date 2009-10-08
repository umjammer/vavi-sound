/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.yamaha;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import vavi.sound.adpcm.Codec;
import vavi.util.Debug;


/**
 * YAMAHA MA#.
 * 
 * @author Furuhon 19-Apl-2004 Rev.1.00
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 1.10 050402 nsano port java <br>
 */
class Yamaha implements Codec {

    /** */
    private class Status {
        int index = 127;
        int last = 0;
    }

    /** You need initialize before reuse methods! */
    private Status stat = new Status();

    /**
     * @param code ADPCM (LSB 4bit —LŒø)
     * @param ss ?
     * @return adjusted ss
     */
    private int adjust(int code, int ss) {
        switch (code & 0x07) {
        case 0x00:
        case 0x01:
        case 0x02:
        case 0x03:
            ss = (ss * 115) / 128;
            break;
        case 0x04:
            ss = (ss * 307) / 256;
            break;
        case 0x05:
            ss = (ss * 409) / 256;
            break;
        case 0x06:
            ss = (ss * 2);
            break;
        case 0x07:
            ss = (ss * 307) / 128;
            break;
        }

        if (ss < 127) {
            ss = 127;
        }
        if (ss > 32768 * 3 / 4) {
            ss = 32768 * 3 / 4;
        }

        return ss;
    }

    /**
     * @param code ADPCM (LSB 4bit —LŒø)
     * @return 16bit PCM signed
     */
    public int decode(int code) {

        int ss = stat.index;
        int e = ss / 8;
        if ((code & 0x01) != 0) {
            e += ss / 4;
        }
        if ((code & 0x02) != 0) {
            e += ss / 2;
        }
        if ((code & 0x04) != 0) {
            e += ss;
        }
        int diff = (code & 0x08) != 0 ? -e : e;
        int samp = stat.last + diff;

        if (samp > 32767) {
            samp = 32767;
        }
        if (samp < -32768) {
            samp = -32768;
        }

        stat.last = samp;
        stat.index = adjust(code, ss);

        return stat.last;
    }

    /**
     * @param samp 16bit PCM signed
     * @return ADPCM (LSB 4bit —LŒø)
     */
    public int encode(int samp) {
        int diff;
        int ss = stat.index;
        int code = 0x00;
        if ((diff = samp - stat.last) < 0) {
            code = 0x08;
        }
        int e = diff < 0 ? -diff : diff;
        if (e >= ss) {
            code = code | 0x04;
            e -= ss;
        }
        if (e >= ss / 2) {
            code = code | 0x02;
            e -= ss / 2;
        }
        if (e >= ss / 4) {
            code = code | 0x01;
        }

        stat.last = decode(code);
//System.err.printf("%04X -> %02X\n", samp, code);
        return code;
    }

    // -------------------------------------------------------------------------

    /**
     * Input Linear PCM WAV must be 8000Hz, 16bit, mono.
     */
    public static void main(String[] args) throws Exception {

        int sampleRate = 8000;
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

        AudioFormat audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,
                1,
                2,
                sampleRate,
                byteOrder.equals(ByteOrder.BIG_ENDIAN));
        System.err.println(audioFormat);

        InputStream is = new YamahaInputStream(new FileInputStream(args[0]), byteOrder);
        System.err.println("available: " + is.available());

// OutputStream os = new BufferedOutputStream(new FileOutputStream(args[1]));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.addLineListener(new LineListener() {
            public void update(LineEvent ev) {
Debug.println(ev.getType());
                if (LineEvent.Type.STOP == ev.getType()) {
                    System.exit(0);
                }
            }
        });
        line.start();
        byte[] buf = new byte[1024];
        int l = 0;

        while (is.available() > 0) {
            l = is.read(buf, 0, 1024);
            line.write(buf, 0, l);
// os.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
// os.close();
    }
}

/* */
