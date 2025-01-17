/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * MfiSystemTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class MfiSystemTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property(name = "vavi.test.volume.midi")
    float midiVolume = 0.2f;

    @Property
    String mfi = "src/test/resources/test.mld";

    Sequencer sequencer;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

Debug.println("volume: " + volume);
        System.setProperty("vavi.sound.mobile.AudioEngine.volume", String.valueOf(volume));
Debug.println("adpcm volume: " + System.getProperty("vavi.sound.mobile.AudioEngine.volume"));

        sequencer = MfiSystem.getSequencer();
        sequencer.open();
    }

    @AfterEach
    void tearDown() throws Exception {
        sequencer.close();
    }

    @Test
    public void test() throws Exception {
        play();
    }

    void play() throws Exception {
        Path path = Path.of(mfi);
Debug.println("path: " + path);
        CountDownLatch cdl = new CountDownLatch(1);
        Sequence sequence = MfiSystem.getSequence(new BufferedInputStream(Files.newInputStream(path)));
        volume(((Synthesizer) sequencer).getReceiver(), midiVolume); // TODO interlock mid adpcm volume
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(meta -> {
Debug.println(Level.FINE, meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        });
Debug.println(Level.FINE, "START");
        sequencer.start();
        cdl.await();
Debug.println(Level.FINE, "END");
    }

    // ----

    /**
     * Tests this class.
     *
     * usage: java -Djavax.sound.midi.Sequencer="#Real Time Sequencer" MfiSystem mfi_file ...
     */
    public static void main(String[] args) throws Exception {
        MfiSystemTest app = new MfiSystemTest();
        app.setup();
        for (var arg : args) {
            app.mfi = arg;
            app.play();
        }
        app.tearDown();
    }
}
