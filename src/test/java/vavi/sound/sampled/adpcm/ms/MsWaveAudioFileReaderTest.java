/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ms;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static vavi.sound.SoundUtil.volume;


/**
 * MsWaveAudioFileReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class MsWaveAudioFileReaderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

    String inFile = "/vavi/sound/adpcm/ms/ms_8k_4_mono.wav";
    String correctFile = "/vavi/sound/adpcm/ms/out_sox.pcm";
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
Debug.print(outFormat);

        AudioInputStream iais = AudioSystem.getAudioInputStream(getClass().getResource(inFile));
Debug.print("in available: " + iais.available() + ", " + iais.getFormat());
        AudioInputStream oais = AudioSystem.getAudioInputStream(outFormat, iais);
Debug.print("out available: " + oais.available() + ", " + oais.getFormat());

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

    @Test
    @DisplayName("another input type 2")
    void test2() throws Exception {
        URL url = Paths.get("src/test/resources/" + inFile).toUri().toURL();
        AudioInputStream ais = AudioSystem.getAudioInputStream(url);
        assertEquals(MsEncoding.MS, ais.getFormat().getEncoding());
    }

    @Test
    @DisplayName("another input type 3")
    void test3() throws Exception {
        File file = Paths.get("src/test/resources/" + inFile).toFile();
        AudioInputStream ais = AudioSystem.getAudioInputStream(file);
        assertEquals(MsEncoding.MS, ais.getFormat().getEncoding());
    }

    @Test
    @DisplayName("when unsupported file coming")
    void test5() throws Exception {
        InputStream is = MsWaveAudioFileReaderTest.class.getResourceAsStream("/test.caf");
        int available = is.available();
        UnsupportedAudioFileException e = assertThrows(UnsupportedAudioFileException.class, () -> {
Debug.println(is);
            AudioSystem.getAudioInputStream(is);
        });
Debug.println(e.getMessage());
        assertEquals(available, is.available()); // spi must not consume input stream even one byte
    }
}
