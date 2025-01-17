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
import vavi.io.LittleEndianDataInputStream;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavi.util.win32.WAVE;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * ImaOutputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060120 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class ImaInputStreamTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

    String inFile = "ima_8k_4_mono.wav";
    String correctFile = "linear_8k_16_mono.pcm";
    File outFile;

    @BeforeEach
    public void setup() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
Debug.println(Level.FINE, "outFile: " + outFile);
    }

    /**
     * <pre>
     * IMA wave ext
     *  2 bytes
     *  samplesPerBlock (little endian)
     * </pre>
     */
    @Test
    public void test1() throws Exception {

        InputStream in = new BufferedInputStream(getClass().getResourceAsStream(inFile));
        WAVE wave = WAVE.readFrom(in, WAVE.class);
        in.close();
        WAVE.fmt format = wave.findChildOf(WAVE.fmt.class);
        if (format.getFormatId() != 0x0011) {
            throw new IllegalArgumentException("not Intel DVI/IMA ADPCM");
        }
        LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(
            new ByteArrayInputStream(format.getExtended()));
Debug.println("Level.FINE, ext size: " + ledis.available());
        int samplesPerBlock = ledis.readShort();
        ledis.close();
        WAVE.data data = wave.findChildOf(WAVE.data.class);
        in = new ByteArrayInputStream(data.getWave());
Debug.println(Level.FINE, "wave: " + in.available());

        // ----

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
Debug.print(audioFormat);

Debug.print("samplesPerBlock: " + samplesPerBlock + ", numberChannels: " + format.getNumberChannels() + ", blockSize: " + format.getBlockSize());
        InputStream is = new ImaInputStream(in,
                                            samplesPerBlock,
                                            format.getNumberChannels(),
                                            format.getBlockSize(),
                                            byteOrder);
        OutputStream os = new BufferedOutputStream(Files.newOutputStream(outFile.toPath()));

        int bufferSize = format.getBlockSize();

DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
line.open(audioFormat);
line.addLineListener(ev -> {
Debug.println(Level.FINE, ev.getType());
 if (LineEvent.Type.STOP == ev.getType()) {
 }
});
line.start();
volume(line, volume);

        byte[] buf = new byte[bufferSize];
        while (true) {
            int r = is.read(buf, 0, bufferSize);
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
