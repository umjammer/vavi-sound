/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.oki;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;

import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * OkiInputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class OkiInputStreamTest {

    String inFile = "out.adpcm";
    String correctFile = "out.pcm";
    File outFile;

    @BeforeEach
    public void setup() throws IOException {
        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
Debug.println("outFile: " + outFile);
    }

    /** */
    @Test
    public void test1() throws Exception {

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

        InputStream is = new OkiInputStream(getClass().getResourceAsStream(inFile), ByteOrder.LITTLE_ENDIAN);
System.err.println("available: " + is.available());
        OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        volume(line, .2d);

        byte[] buf = new byte[1024];
        while (true) {
            int r = is.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
if (os != null) {
 os.write(buf, 0, r);
}
        }
        line.drain();
        line.stop();
        line.close();
if (os != null) {
 os.close();
}
        is.close();

        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(outFile));
    }
}

/* */
