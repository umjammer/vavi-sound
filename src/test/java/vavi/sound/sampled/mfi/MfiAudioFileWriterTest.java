/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.mfi;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * MfiAudioFileWriterTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@Disabled
public class MfiAudioFileWriterTest {

    @Test
    public void test() {
        fail("Not yet implemented");
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
        String model = args[4];
        String nullDevice = args[5]; // "nul" or "/dev/null"
        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(inFilename));
System.err.println(ais.getFormat());
        Map<String, Object> properties = new HashMap<>();
        properties.put("mfi.divided", true);
        properties.put("mfi.directory", outDir);
        properties.put("mfi.base", base);
        properties.put("mfi.model", model);
        properties.put("mfi.time", time);
        properties.put("mfi.sampleRate", 8000);
        properties.put("mfi.bits", 4);
        properties.put("mfi.channels", 1);
        properties.put("mfi.masterVolume", 100);
        properties.put("mfi.adpcmVolume", 100);
        AudioSystem.write(ais, new MFi(properties), new File(nullDevice));
        System.exit(0);
    }
}

/* */
