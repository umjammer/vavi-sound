/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ima;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vavi.io.InputEngineOutputStream;
import vavi.io.LittleEndianDataInputStream;
import vavi.io.OutputEngineInputStream;
import vavi.util.Debug;
import vavix.io.IOStreamInputEngine;
import vavix.io.IOStreamOutputEngine;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * ImaOutputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class ImaOutputStreamTest {

    static final double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    String inFile = "out.pcm";
    String correctFile = "out_sox.adpcm";
    File outFile;

    @BeforeEach
    public void setup() throws IOException {
        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
//        outFile = new File("src/test/resources/vavi/sound/adpcm/ima/out_vavi.adpcm");
Debug.println(Level.FINE, "outFile: " + outFile.getCanonicalPath());
    }

    /** */
    @Test
    public void test1() throws Exception {
        OutputStream os = new BufferedOutputStream(Files.newOutputStream(outFile.toPath()));
        InputStream in = new BufferedInputStream(getClass().getResourceAsStream(inFile));
        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(in,
                out -> new ImaOutputStream(out, 505, 1)));
        byte[] buffer = new byte[505 * 2];
        while (true) {
            int amount = is.read(buffer);
//System.err.println("amount: " + amount);
            if (amount < 0) {
                break;
            }
            os.write(buffer, 0, amount);
            os.flush();
        }
        os.close();
        is.close();

        InputStream is2 = new ImaInputStream(Files.newInputStream(outFile.toPath()),
                                             505,
                                             1,
                                             256);

        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                  8000,
                                                  16,
                                                  1,
                                                  2,
                                                  8000,
                                                  true);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();

        volume(line, volume);

        byte[] buf = new byte[8192];
        while (true) {
            int r = is2.read(buf, 0, 8192);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }

        line.drain();
        line.stop();
        line.close();

        is2.close();
Debug.println(Level.FINE, "outFile: " + outFile.length());
        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(outFile));
    }

    /**
     * TODO input engine と output engine の結合がうまくいかない
     */
    @Test
    @Disabled
    public void test2() throws Exception {
//        final String inFile = "out.pcm";
//        final String outFile = "src/test/resources/vavi/sound/adpcm/ima/out_vavi.pcm";

        InputStream ris = getClass().getResourceAsStream(inFile);
        InputStream is = new OutputEngineInputStream(new IOStreamOutputEngine(ris,
                out -> new ImaOutputStream(out, 505, 1), 1010));
        OutputStream fos = Files.newOutputStream(outFile.toPath());
        OutputStream os = new InputEngineOutputStream(new IOStreamInputEngine(fos,
                in -> new ImaInputStream(in, 505, 1, 256), 1010));
        byte[] buffer = new byte[505 * 2];
        while (true) {
            int amount = is.read(buffer);
            if (amount == 0) {
                continue;
            }
            if (amount < 0) {
                break;
            }
            os.write(buffer, 0, amount);
        }
        os.flush();
        os.close();
        is.close();

        InputStream is2 = Files.newInputStream(outFile.toPath());

        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                  8000,
                                                  16,
                                                  1,
                                                  2,
                                                  8000,
                                                  true);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();

        volume(line, volume);

        byte[] buf = new byte[8192];
        while (true) {
            int r = is2.read(buf, 0, 8192);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }

        line.drain();
        line.stop();
        line.close();

        is2.close();
Debug.println(Level.FINE, "outFile: " + outFile.length());
        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(inFile)), Checksum.getChecksum(outFile));
    }

    /** */
    @Test
    public void test3() throws Exception {
        final String inFile = "out.pcm";
        final String outFile = "src/test/resources/vavi/sound/adpcm/ima/out_vavi.adpcm";

        InputStream in = new BufferedInputStream(getClass().getResourceAsStream(inFile));
        OutputStream os = new BufferedOutputStream(Files.newOutputStream(Paths.get(outFile)));

        Ima encoder = new Ima();
        int[] steps = new int[16];

        int length = in.available();
        int spb = 505;
        int ll = 0;
Debug.println(Level.FINE, "inFile: " + length);
        while (ll < length) {
            int bpb = Ima.getBytesPerBlock(1, spb);
            byte[] buffer = new byte[spb * 2];
            int l = 0;
            while (l < buffer.length) {
                int r = in.read(buffer, l, buffer.length - l);
                if (r < 0) {
                    break;
                }
                l += r;
            }
            if (l > 0) {
                byte[] adpcm = new byte[bpb];
                int[] pcm = new int[l / 2];
                LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(new ByteArrayInputStream(buffer));
                for (int i = 0; i < pcm.length; i++) {
                    pcm[i] = ledis.readShort();
                }
                encoder.encodeBlock(1, pcm, pcm.length, steps, adpcm, 9);

                os.write(adpcm);
            }

            ll += l;
        }

        os.flush();
        os.close();

        InputStream is = new ImaInputStream(Files.newInputStream(Paths.get(outFile)),
                                            505,
                                            1,
                                            256);

        AudioFormat audioFormat = new AudioFormat(
                                                  AudioFormat.Encoding.PCM_SIGNED,
                                                  8000,
                                                  16,
                                                  1,
                                                  2,
                                                  8000,
                                                  true);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();

        volume(line, volume);

        byte[] buf = new byte[8192];
        while (true) {
            int r = is.read(buf, 0, 8192);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }

        line.drain();
        line.stop();
        line.close();

        is.close();
Debug.println(Level.FINE, "outFile: " + new File(outFile).length());
        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(new File(outFile)));
    }
}
