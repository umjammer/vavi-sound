/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.yamaha;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import org.junit.Before;
import org.junit.Test;

import vavi.io.IOStreamOutputEngine;
import vavi.io.OutputEngineInputStream;
import vavi.util.Debug;
import vavix.util.Checksum;

import static org.junit.Assert.assertEquals;


/**
 * YamahaOutputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class YamahaOutputStreamTest {

    String inFile = "pcm_8k_16_mono.pcm";
    String correctFile = "out.adpcm";
    File outFile;

    @Before
    public void setup() throws IOException {
        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
Debug.println("outFile: " + outFile);
    }

    /** */
    @Test
    public void test1() throws Exception {
        OutputStream os = new FileOutputStream(outFile);
        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(getClass().getResourceAsStream(inFile), new IOStreamOutputEngine.OutputStreamFactory() {
            public OutputStream getOutputStream(OutputStream out) throws IOException {
                return new YamahaOutputStream(out, ByteOrder.LITTLE_ENDIAN);
            }
        }));
        byte[] buffer = new byte[8192];
        while (true) {
            int amount = is.read(buffer);
            if (amount < 0) {
                break;
            }
            os.write(buffer, 0, amount);
        }
        is.close();
        os.flush();
        os.close();

        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(outFile));
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
