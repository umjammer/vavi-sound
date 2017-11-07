/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.ssrc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import org.junit.Before;
import org.junit.Test;

import vavi.io.LittleEndianDataInputStream;
import vavi.io.LittleEndianDataOutputStream;
import vavi.util.StringUtil;
import vavix.util.Checksum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * SSRCTest.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060127 nsano initial version <br>
 */
public class SSRCTest {

    String inFile;
    String outFile = "out.vavi.wav";
    String correctFile = "out.wav";

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.load(SSRCTest.class.getResourceAsStream("local.properties"));
        inFile = props.getProperty("ssrc.in.wav");
System.err.println(inFile);
    }

    /** down sample */
    public void $test1() throws Exception {
        SSRC.main(new String[] { "--rate", "8000", "--twopass", "--normalize", inFile, outFile });

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(outFile));
        AudioFormat format = ais.getFormat();
System.err.println(format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
        line.start();
        byte[] buf = new byte[1024];
        int l;
        while (ais.available() > 0) {
            l = ais.read(buf, 0, 1024);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    /** up sample */
    public void $test3() throws Exception {
        SSRC.main(new String[] { "--rate", "48000", "--twopass", "--normalize", inFile, outFile });

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(outFile));
        AudioFormat format = ais.getFormat();
System.err.println(format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
        line.start();
        byte[] buf = new byte[1024];
        int l;
        while (ais.available() > 0) {
            l = ais.read(buf, 0, 1024);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    /** down sample (nio) */
    public void $test4() throws Exception {
        SSRC.main(new String[] { "--rate", "8000", "--twopass", "--normalize", inFile, outFile });

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(outFile));
        AudioFormat format = ais.getFormat();
System.err.println(format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
        line.start();
        byte[] buf = new byte[1024];
        int l;
        while (ais.available() > 0) {
            l = ais.read(buf, 0, 1024);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    /** up sample (nio) */
    @Test
    public void test5() throws Exception {
        SSRC.main(new String[] { "--rate", "44100", "--twopass", "--normalize", inFile, outFile });

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(outFile));
        AudioFormat format = ais.getFormat();
System.err.println(format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
        line.start();
        byte[] buf = new byte[1024];
        int l;
        while (ais.available() > 0) {
            l = ais.read(buf, 0, 1024);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    /** */
    public void $test2() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianDataOutputStream leos = new LittleEndianDataOutputStream(baos);
        leos.writeDouble(0.123456789);
        leos.close();
System.err.println("1:\n" + StringUtil.getDump(baos.toByteArray()));
        //
        byte[] buf = new byte[8];
        writeDouble(buf, 0, 0.123456789);
System.err.println("2:\n" + StringUtil.getDump(buf));
        assertTrue(Arrays.equals(baos.toByteArray(), buf));
        //
        LittleEndianDataInputStream leis = new LittleEndianDataInputStream(new ByteArrayInputStream(buf));
        double d = leis.readDouble();
        leis.close();
System.err.printf("3: %f\n", d);
        assertEquals(0.123456789, d, 0);
    }

    /**
     * @param buffer
     * @param offset
     * @param value
     */
    private final void writeDouble(byte[] buffer, int offset, double value) {
        long l = Double.doubleToLongBits(value);
        buffer[offset * 8 + 0] = (byte)  (l & 0x00000000000000ffl);
        buffer[offset * 8 + 1] = (byte) ((l & 0x000000000000ff00l) >>  8);
        buffer[offset * 8 + 2] = (byte) ((l & 0x0000000000ff0000l) >> 16);
        buffer[offset * 8 + 3] = (byte) ((l & 0x00000000ff000000l) >> 24);
        buffer[offset * 8 + 4] = (byte) ((l & 0x000000ff00000000l) >> 32);
        buffer[offset * 8 + 5] = (byte) ((l & 0x0000ff0000000000l) >> 40);
        buffer[offset * 8 + 6] = (byte) ((l & 0x00ff000000000000l) >> 48);
        buffer[offset * 8 + 7] = (byte) ((l & 0xff00000000000000l) >> 56);
    }
}

/* */
