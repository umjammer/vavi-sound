/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import junit.framework.TestCase;

import vavi.sound.Checksum;


/**
 * G721InputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class G721InputStreamTest extends TestCase {

    String inFile = "out.4.adpcm";
    String outFile = "out.vavi.4.pcm";
    String correctFile = "out.4.pcm";

    /** */
    public void test1() throws Exception {
        decode(new String[] { inFile, outFile, "test" });

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }


    /**
     * Play G721 ADPCM.
     * @param args 0:dvi adpcm, 1:output pcm, 2:test or not, use "test"
     */
    void decode(String[] args) throws Exception {

        final boolean isTest = args[2].equals("test");

        int sampleRate = 8000;
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println(format);

        InputStream is = new G721InputStream(new FileInputStream(args[0]), ByteOrder.LITTLE_ENDIAN);
OutputStream os = null;
if (args[1] != null) {
 System.err.println("available: " + is.available());
 os = new BufferedOutputStream(new FileOutputStream(args[1]));
}

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        byte[] buf = new byte[1024];
        int l = 0;
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);

        while (is.available() > 0) {
            l = is.read(buf, 0, 1024);
            line.write(buf, 0, l);
if (os != null) {
 os.write(buf, 0, l);
}
        }
        line.drain();
        line.stop();
        line.close();
if (os != null) {
 os.close();
}
        if (!isTest) {
            System.exit(0);
        }
    }
}

/* */
