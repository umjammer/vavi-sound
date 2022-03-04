/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.smaf;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * SmafAudioFileWriterTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class SmafAudioFileWriterTest {

    String nullDevice = System.getProperty("vavi.test.dev.null");

    static Path dir;

    @BeforeAll
    static void setup() throws Exception {
        dir = Paths.get("tmp");
        if (Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    /**
     * mono wave -> smaf
     */
    @Test
    public void test0() throws Exception {
        Path path = Paths.get(SmafAudioFileWriterTest.class.getResource("/mono.wav").toURI());
        AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));
        Map<String, Object> properties = new HashMap<>();
        properties.put("smaf.divided", true);
        properties.put("smaf.directory", dir.toString());
        properties.put("smaf.base", path.getFileName().toString().replace(".wav", "_%d.mmf"));
        properties.put("smaf.time", (float) 10.0);
        properties.put("smaf.sampleRate", 8000);
        properties.put("smaf.bits", 4);
        properties.put("smaf.channels", 1);
        properties.put("smaf.masterVolume", 100);
        properties.put("smaf.adpcmVolume", 100);
        new SmafAudioFileWriter().write(ais, new SMAF(properties), new File(nullDevice));
        ais.close();
    }

    @Test
    public void test1() throws Exception {
        Path path = Paths.get(SmafAudioFileWriterTest.class.getResource("/mono.wav").toURI());
        AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));
        Map<String, Object> properties = new HashMap<>();
        properties.put("smaf.divided", true);
        properties.put("smaf.directory", dir.toString());
        properties.put("smaf.base", path.getFileName().toString().replace(".wav", "_%d.mmf"));
        properties.put("smaf.time", (float) 10.0);
        properties.put("smaf.sampleRate", 8000);
        properties.put("smaf.bits", 4);
        properties.put("smaf.channels", 1);
        properties.put("smaf.masterVolume", 100);
        properties.put("smaf.adpcmVolume", 100);
        AudioSystem.write(ais, new SMAF(properties), new File(nullDevice));
        ais.close();
    }

    //----

    /**
     * @param args 0: input PCM, 1: output base dir, 2: length in seconds, 3: base file name, 4: type, 5: null device
     */
    public static void main(String[] args) throws Exception {
        String inFilename = args[0];
        String outDir = args[1];
        float time = Float.parseFloat(args[2]);
        String base = args[3];
        String nullDevice = args[4]; // "nul" or "/dev/null"
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(inFilename));
        Map<String, Object> properties = new HashMap<>();
        properties.put("smaf.divided", true);
        properties.put("smaf.directory", outDir);
        properties.put("smaf.base", base);
        properties.put("smaf.time", time);
        properties.put("smaf.sampleRate", 8000);
        properties.put("smaf.bits", 4);
        properties.put("smaf.channels", 1);
        properties.put("smaf.masterVolume", 100);
        properties.put("smaf.adpcmVolume", 100);
        AudioSystem.write(ais, new SMAF(properties), new File(nullDevice));
        ais.close();
    }
}

/* */
