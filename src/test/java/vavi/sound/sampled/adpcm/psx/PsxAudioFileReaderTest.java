/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.psx;

import java.io.BufferedInputStream;
import java.io.File;
import java.lang.System.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static java.lang.System.getLogger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static vavi.sound.SoundUtil.volume;


/**
 * PsxAudioFileReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-03 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class PsxAudioFileReaderTest {

    private static final Logger logger = getLogger(PsxAudioFileReaderTest.class.getName());

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

    @Property(name = "adpcm.psx")
    String adpcm = "src/test/resources/test.psx";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

Debug.print("volume: " + volume);
    }

    @Test
    @EnabledIf("localPropertiesExists")
    @DisplayName("detection by file and by file backed stream")
    void test1() throws Exception {
        Path path = Path.of(adpcm);
        assumeTrue(Files.exists(path), "no psx test file: " + path);

        // by file
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(path.toFile());
        AudioFormat format = fileFormat.getFormat();
Debug.println("file: " + format);
        assertEquals(PsxEncoding.PSX, format.getEncoding());
        if (adpcm.toLowerCase().endsWith(".mib")) {
            assertEquals(44100, (int) format.getSampleRate());
        } else if (adpcm.toLowerCase().endsWith(".mi4")) {
            assertEquals(48000, (int) format.getSampleRate());
        }
        assertNotNull(format.getProperty("interleave"));
        assertNotNull(format.getProperty("numSamples"));

        // by stream, exercises SoundUtil#getSource
        AudioFileFormat fileFormat2;
        try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(path))) {
            fileFormat2 = AudioSystem.getAudioFileFormat(stream);
        }
Debug.println("stream: " + fileFormat2.getFormat());
        assertEquals(PsxEncoding.PSX, fileFormat2.getFormat().getEncoding());
        assertEquals(format.getChannels(), fileFormat2.getFormat().getChannels());
    }

    @Test
    @DisplayName("non psx contents are not detected")
    void test2() throws Exception {
        PsxAudioFileReader reader = new PsxAudioFileReader();
        // wrong extension
        File vox = new File("src/test/resources/vavi/sound/adpcm/vox/out.adpcm");
        assertThrows(UnsupportedAudioFileException.class, () -> reader.getAudioFileFormat(vox));
        // not a file backed stream
        assertThrows(UnsupportedAudioFileException.class, () ->
                reader.getAudioFileFormat(new java.io.ByteArrayInputStream(new byte[0x100])));
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    @DisplayName("spi playback")
    void test3() throws Exception {
        Path path = Path.of(adpcm);
Debug.println("path: " + path + ", " + Files.size(path));

        AudioInputStream ais = AudioSystem.getAudioInputStream(path.toFile());
        AudioFormat inFormat = ais.getFormat();
Debug.println("in: " + inFormat);

        AudioFormat outFormat = new AudioFormat(inFormat.getSampleRate(), 16, inFormat.getChannels(), true, false);
        AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outFormat, ais);
Debug.println("out: " + pcmAis.getFormat());

        SourceDataLine line = AudioSystem.getSourceDataLine(outFormat);
        line.open(outFormat);
        line.start();
        volume(line, volume);

        byte[] buf = new byte[8192];
        int l;
        while ((l = pcmAis.read(buf, 0, buf.length)) != -1) {
            line.write(buf, 0, l);
        }

        line.drain();
        line.stop();
        line.close();
        pcmAis.close();
    }
}
