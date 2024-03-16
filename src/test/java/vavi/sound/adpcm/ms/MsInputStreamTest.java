/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.logging.Level;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vavi.io.LittleEndianDataInputStream;
import vavi.util.Debug;
import vavi.util.win32.WAVE;

import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * MsInputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
public class MsInputStreamTest {

    static final double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    String inFile = "ms_8k_4_mono.wav";
    String correctFile = "out_sox.pcm";
    File outFile;

    @BeforeEach
    public void setup() throws IOException {
        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
Debug.println(Level.FINE, "outFile: " + outFile);
    }

    /**
     * <pre>
     * MS wave ext
     *  2 bytes samplesPerBlock (little endian)
     *  2 bytes nCoefs
     *  2 * nCoefs iCoefs
     * </pre>
     */
    @Test
    public void test1() throws Exception {

        InputStream in = new BufferedInputStream(getClass().getResourceAsStream(inFile));
        WAVE wave = WAVE.readFrom(in, WAVE.class);
        in.close();
        WAVE.fmt format = wave.findChildOf(WAVE.fmt.class);
        if (format.getFormatId() != 0x0002) {
            throw new IllegalArgumentException("not Microsoft ADPCM");
        }
        LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(new ByteArrayInputStream(format.getExtended()));
Debug.println(Level.FINE, "ext size: " + ledis.available());
        int samplesPerBlock = ledis.readShort();
        int nCoefs = ledis.readShort();
Debug.println(Level.FINE, "nCoefs: " + nCoefs);
        int[][] iCoefs = new int[nCoefs][2];
        for (int i = 0; i < nCoefs; i++) {
            for (int j = 0; j < 2; j++) {
                iCoefs[i][j] = ledis.readShort();
Debug.printf(Level.FINE, "iCoef[%d][%d]: %04x: %d\n", i, j, iCoefs[i][j] & 0xffff, iCoefs[i][j] & 0xffff);
            }
        }
        ledis.close();
        WAVE.data data = wave.findChildOf(WAVE.data.class);
        in = new ByteArrayInputStream(data.getWave());
Debug.println(Level.FINE, "wave: " + in.available());

        //----

        int sampleRate = format.getSamplingRate();
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println(audioFormat);

Debug.printf(Level.FINE, "samplesPerBlock: %d, numberChannels: %d, blockSize: %d\n", samplesPerBlock, format.getNumberChannels(), format.getBlockSize());
        InputStream is = new MsInputStream(in,
                                           samplesPerBlock,
                                           nCoefs,
                                           iCoefs,
                                           format.getNumberChannels(),
                                           format.getBlockSize(),
                                           byteOrder);
        OutputStream os = new BufferedOutputStream(Files.newOutputStream(outFile.toPath()));

DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
line.open(audioFormat);
line.addLineListener(ev -> Debug.println(Level.FINE, ev.getType()));
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
}
