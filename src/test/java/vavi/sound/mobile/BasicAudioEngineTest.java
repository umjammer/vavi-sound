/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import vavi.sound.mfi.MfiSystemTest;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;


/**
 * BasicAudioEngineTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class BasicAudioEngineTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @BeforeEach
    public void setup() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        System.setProperty("vavi.sound.mobile.AudioEngine.volume", String.valueOf(volume));
Debug.println("adpcm volume: " + System.getProperty("vavi.sound.mobile.AudioEngine.volume"));
    }

    @Test
    public void test() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);
        Path inPath = Paths.get(MfiSystemTest.class.getResource("/test.mld").toURI());
        vavi.sound.mfi.Sequencer sequencer = vavi.sound.mfi.MfiSystem.getSequencer();
        sequencer.open();
        vavi.sound.mfi.Sequence sequence = vavi.sound.mfi.MfiSystem.getSequence(new BufferedInputStream(Files.newInputStream(inPath)));
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(meta -> {
Debug.println(meta.getType());
            if (meta.getType() == 47) {
                cdl.countDown();
            }
        });
        sequencer.start();
        cdl.await();
        sequencer.close();
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    public void test2() throws Exception {
        YamahaAudioEngine audioEngine = new YamahaAudioEngine();
        byte[] data = Files.readAllBytes(Path.of("tmp", "exwv.bin"));
        audioEngine.setData(1, 1, 8000, 4, 1, data, false);
        audioEngine.start(1);
        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await(10, TimeUnit.MINUTES);
    }

    // ----

    /** */
    private static String fileName;

    /** */
    private static String pcmFileName;

    /**
     * Tests this class.
     *
     * usage: java $0 mfi_file adpcm
     */
    public static void main(String[] args) throws Exception {

        if (args.length >= 2) {
            fileName = args[1];
        }
        if (args.length >= 3) {
            pcmFileName = args[2];
        }

        CountDownLatch cdl = new CountDownLatch(1);
        vavi.sound.mfi.Sequencer sequencer = vavi.sound.mfi.MfiSystem.getSequencer();
        sequencer.open();
        vavi.sound.mfi.Sequence sequence = vavi.sound.mfi.MfiSystem.getSequence(new File(args[0]));
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(meta -> {
Debug.println(meta.getType());
            if (meta.getType() == 47) {
                cdl.countDown();
            }
        });
        sequencer.start();
        cdl.await();
        sequencer.close();
    }
}
