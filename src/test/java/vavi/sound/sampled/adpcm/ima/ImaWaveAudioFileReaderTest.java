/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ima;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * ImaWaveAudioFileReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class ImaWaveAudioFileReaderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

    String inFile = "/vavi/sound/adpcm/ima/ima_8k_4_mono.wav";
    String correctFile = "/vavi/sound/adpcm/ima/linear_8k_16_mono.pcm";
    File outFile;

    @BeforeEach
    public void setup() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        outFile = File.createTempFile("vavi", ".pcm");
        outFile.deleteOnExit();
Debug.println("outFile: " + outFile);
    }

    @Test
    public void test0() throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(ImaWaveAudioFileReader.class.getResource(inFile));
        Arrays.stream(AudioSystem.getAudioFileTypes(ais)).forEach(System.err::println);
    }

    @Test
    @DisplayName("extract from wav")
    public void test1() throws Exception {

        int sampleRate = 8000;
        ByteOrder byteOrder = ByteOrder.nativeOrder();

        AudioFormat outFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println(outFormat);

        AudioInputStream iais = AudioSystem.getAudioInputStream(getClass().getResourceAsStream(inFile));
System.err.println("in available: " + iais.available() + ", " + iais.getFormat());
        AudioInputStream oais = AudioSystem.getAudioInputStream(outFormat, iais);
System.err.println("out available: " + oais.available() + ", " + oais.getFormat());

        OutputStream os = new BufferedOutputStream(Files.newOutputStream(outFile.toPath()));

DataLine.Info info = new DataLine.Info(SourceDataLine.class, outFormat);
SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
line.open(outFormat);
line.start();
volume(line, volume);

        byte[] buf = new byte[1024];
        int l;

        while (oais.available() > 0) {
            l = oais.read(buf, 0, 1024);
line.write(buf, 0, l);
            os.write(buf, 0, l);
        }
line.drain();
line.stop();
line.close();
        os.close();

        assertEquals(Checksum.getChecksum(getClass().getResourceAsStream(correctFile)), Checksum.getChecksum(outFile));
    }
}
