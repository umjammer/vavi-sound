/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.vox;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * VoxInputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class VoxInputStreamTest {

    static final double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    String inFile = "out.adpcm";
    String correctFile = "out.pcm";
    File outFile;

    @BeforeEach
    public void setup() throws IOException {
        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
Debug.println(Level.FINE, "outFile: " + outFile);
    }

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

        InputStream is = new VoxInputStream(getClass().getResourceAsStream(inFile), ByteOrder.LITTLE_ENDIAN);
System.err.println("available: " + is.available());
        OutputStream os = new BufferedOutputStream(Files.newOutputStream(outFile.toPath()));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        volume(line, volume);

        byte[] buf = new byte[1024];
        while (true) {
            int r = is.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
            os.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
        os.close();
        is.close();
        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(outFile));
    }

    // ----

    /** */
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

        InputStream is = new VoxInputStream(Files.newInputStream(Paths.get(args[0])), byteOrder);
System.err.println("available: " + is.available());

//OutputStream os = new BufferedOutputStream(new FileOutputStream(args[1]));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.addLineListener(ev -> {
Debug.println(ev.getType());
            if (LineEvent.Type.STOP == ev.getType()) {
                System.exit(0);
            }
        });
        line.start();
        byte[] buf = new byte[1024];
        int l;

        while (is.available() > 0) {
            l = is.read(buf, 0, 1024);
            line.write(buf, 0, l);
//os.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
//os.close();
        is.close();
    }
}
