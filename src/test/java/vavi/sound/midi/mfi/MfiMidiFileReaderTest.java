/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * MfiMidiFileReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class MfiMidiFileReaderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    float volume = 0.2f;

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
        InputStream is = MfiVaviSequenceTest.class.getResourceAsStream("/test.mld");

        play(is);
    }

    void play(InputStream is) throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);

        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();
        Sequence sequence = MidiSystem.getSequence(is);
Debug.println(sequence);
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

    // ----

    /**
     * Play MFi file.
     * @param args [0] filename
     */
    public static void main(String[] args) throws Exception {
        MfiMidiFileReaderTest app = new MfiMidiFileReaderTest();
        app.setup();
        app.play(Files.newInputStream(Path.of(args[0])));
    }
}
