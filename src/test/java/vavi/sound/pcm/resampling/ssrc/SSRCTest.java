/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.ssrc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vavi.io.LittleEndianDataInputStream;
import vavi.io.LittleEndianDataOutputStream;
import vavi.util.Debug;
import vavi.util.StringUtil;

import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * SSRCTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060127 nsano initial version <br>
 */
public class SSRCTest {

//    static String inFile = "/Users/nsano/Music/0/kirameki01.wav";  // TODO error in down sampling
    static String inFile = "src/test/resources/vavi/sound/pcm/resampling/ssrc/44100.wav";
//    static String inFile = "/Users/nsano/Music/0/rc.wav";

    static String outFile = "tmp/out.vavi.wav";
    static String correctFile = "src/test/resources/vavi/sound/pcm/resampling/ssrc/out.wav";

    static boolean onIde;

    @BeforeAll
    public static void setUp() throws Exception {
        onIde = System.getProperty("vavi.test", "").equals("ide");
Debug.println("onIde: " + onIde + ", " + System.getProperty("vavi.test") + ", file: " + inFile);
    }

    @Test
    @Disabled("ssrc uses random, so check sum never be equal")
    @DisplayName("down sample, call by main")
    public void test1() throws Exception {
        SSRC.main(new String[] { "--rate", "8000", "--twopass", "--normalize", inFile, outFile });

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(outFile));
        AudioFormat format = ais.getFormat();
Debug.println(format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        volume(line, .2d);
        line.start();
        byte[] buf = new byte[1024];
        int l;
        while (ais.available() > 0) {
            l = ais.read(buf, 0, buf.length);
            if (onIde)
                line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    static String outFile3 = "tmp/out3.vavi.wav";
    static String correctFile3 = "src/test/resources/vavi/sound/pcm/resampling/ssrc/out.wav";

    @Test
    @Disabled("ssrc uses random, so check sum never be equal")
    @DisplayName("up sample, call by main")
    public void test3() throws Exception {
        SSRC.main(new String[] { "--rate", "48000", "--twopass", "--normalize", inFile, outFile });

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(outFile));
        AudioFormat format = ais.getFormat();
Debug.println(format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        volume(line, .2d);
        line.start();
        byte[] buf = new byte[1024];
        int l;
        while (ais.available() > 0) {
            l = ais.read(buf, 0, buf.length);
            if (onIde)
                line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();

        assertEquals(Checksum.getChecksum(new File(correctFile3)), Checksum.getChecksum(new File(outFile)));
    }

    @Test
    public void test2() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianDataOutputStream leos = new LittleEndianDataOutputStream(baos);
        leos.writeDouble(0.123456789);
        leos.close();
Debug.println("1:\n" + StringUtil.getDump(baos.toByteArray()));
        //
        byte[] buf = new byte[8];
        writeDouble(buf, 0, 0.123456789);
Debug.println("2:\n" + StringUtil.getDump(buf));
        assertArrayEquals(baos.toByteArray(), buf);
        //
        LittleEndianDataInputStream leis = new LittleEndianDataInputStream(new ByteArrayInputStream(buf));
        double d = leis.readDouble();
        leis.close();
Debug.printf("3: %f\n", d);
        assertEquals(0.123456789, d, 0.000000001);
    }

    /** */
    private void writeDouble(byte[] buffer, int offset, double value) {
        long l = Double.doubleToLongBits(value);
        buffer[offset * 8 + 0] = (byte)  (l & 0x00000000000000ffL);
        buffer[offset * 8 + 1] = (byte) ((l & 0x000000000000ff00L) >>  8);
        buffer[offset * 8 + 2] = (byte) ((l & 0x0000000000ff0000L) >> 16);
        buffer[offset * 8 + 3] = (byte) ((l & 0x00000000ff000000L) >> 24);
        buffer[offset * 8 + 4] = (byte) ((l & 0x000000ff00000000L) >> 32);
        buffer[offset * 8 + 5] = (byte) ((l & 0x0000ff0000000000L) >> 40);
        buffer[offset * 8 + 6] = (byte) ((l & 0x00ff000000000000L) >> 48);
        buffer[offset * 8 + 7] = (byte) ((l & 0xff00000000000000L) >> 56);
    }

    @Test
    @DisplayName("call by stream")
    public void test4() throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat format = ais.getFormat();
        AudioFormat outFormat = new AudioFormat(
            format.getEncoding(),
            8000,
            format.getSampleSizeInBits(),
            format.getChannels(),
            format.getFrameSize(),
            format.getFrameRate(),
            format.isBigEndian());
Debug.println(format);
Debug.println(outFormat);

        InputStream in = new SSRCInputStream(format, outFormat, ais);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, outFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(outFormat);
        // volume
        volume(line, .2d);
        line.start();
        byte[] buf = new byte[0x10000];
        int f = format.getFrameSize() * format.getChannels();
//Debug.println("frame: " + f);
outer:
        while (true) {
            int l = 0, a, b = 0;
            // SSRCInputStream is async class
            // so we need to wait data buffer will be filled.
            while (l < 4096) {
                 int r = in.read(buf, l + b, buf.length - (l + b));
                 if (r < 0)
                     break outer;
                 l += r;
            }
//Debug.println(l);
            // we need to keep line.write buffer size is multiply of "f"
            a = l / f * f;
            b = l % f;
            if (onIde)
                line.write(buf, 0, a);
            System.arraycopy(buf, 0, buf, a, b);
        }
        in.close();
        line.drain();
        line.stop();
        line.close();
    }
}

/* */
