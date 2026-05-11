/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import vavi.sound.sampled.misc.HijackSourceDataLine;
import vavi.util.Debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * SoundUtilTest.
 * <p>
 * java runtime option
 * <ol>
 *  <ul>{@code --add-opens=java.base/java.io=ALL-UNNAMED}</ul>
 *  <ul>{@code --add-opens=java.base/sun.nio.ch=ALL-UNNAMED}</ul>
 * </ol>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 241129 nsano initial version <br>
 */
class SoundUtilTest {

    @Test
    void test1() throws Exception {
        InputStream is = SoundUtil.class.getResourceAsStream("/test.mid");
        URI uri = SoundUtil.getSource(is);
        assertNotNull(uri);
        assertTrue(uri.toString().endsWith("target/test-classes/test.mid"));

        is = new BufferedInputStream(Files.newInputStream(Path.of("src/test/resources/test.mid")));
        uri = SoundUtil.getSource(is);
        assertNotNull(uri);
        assertTrue(uri.toString().endsWith("src/test/resources/test.mid"));

        is = new BufferedInputStream(new FileInputStream("src/test/resources/test.mid"));
        uri = SoundUtil.getSource(is);
        assertNotNull(uri);
        assertTrue(uri.toString().endsWith("src/test/resources/test.mid"));

        is = new BufferedInputStream(Files.newInputStream(Path.of(SoundUtil.class.getResource("/test.mid").toURI())));
        uri = SoundUtil.getSource(is);
        assertNotNull(uri);
        assertTrue(uri.toString().endsWith("target/test-classes/test.mid"));
    }

    @Test
    void test2() throws Exception {
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        SourceDataLine sdl = AudioSystem.getSourceDataLine(format);
Debug.print("AudioSystem.getSourceDataLine: " + sdl.getClass().getName());

        System.setProperty("javax.sound.sampled.SourceDataLine", "#Hijack Mixer");

        sdl = AudioSystem.getSourceDataLine(format);
        assertInstanceOf(HijackSourceDataLine.class, sdl);

        assertDoesNotThrow(() -> {
            SourceDataLine line = SoundUtil.getLine("#Default Audio Device", SourceDataLine.class);
Debug.print(line.getClass().getName());
            assertEquals("com.sun.media.sound.DirectAudioDevice$DirectSDL", line.getClass().getName());
        });

        System.setProperty("javax.sound.sampled.SourceDataLine", "");
    }
}
