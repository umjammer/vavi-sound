/*
 * Copyright (c) 2009 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.sound.smaf.MetaEventListener;
import vavi.sound.smaf.Sequence;
import vavi.sound.smaf.Sequencer;
import vavi.sound.smaf.SmafSystem;
import vavi.sound.smaf.Synthesizer;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * test smaf (raw SMAF API).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 090913 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class PlaySMAF {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static {
        // should use this, but volume not works?
//        System.setProperty("javax.sound.midi.Sequencer", "vavi.sound.midi.VaviSequencer");
        System.setProperty("javax.sound.midi.Sequencer", "#Real Time Sequencer");
    }

    @Property(name = "vavi.test.volume.midi")
    float volume = 0.2f;

    @Property(name = "sf2")
    String sf2 = System.getProperty("user.home") + "/Library/Audio/Sounds/Banks/Orchestra/default.sf2";

    @Property
    String mmf = "src/test/resources/test.mmf";

    private Sequencer sequencer;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        sequencer = SmafSystem.getSequencer();
Debug.println(sequencer.getClass().getName());
        sequencer.open();

Synthesizer synthesizer = (Synthesizer) sequencer;
// sf
Path sf2Path = Path.of(sf2);
if (Files.exists(sf2Path)) {
 Soundbank soundbank = synthesizer.getDefaultSoundbank();
//Instrument[] instruments = synthesizer.getAvailableInstruments();
 Debug.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
 synthesizer.unloadAllInstruments(soundbank);
 soundbank = MidiSystem.getSoundbank(sf2Path.toFile());
 synthesizer.loadAllInstruments(soundbank);
//instruments = synthesizer.getAvailableInstruments();
 Debug.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
}
    }

    @AfterEach
    void tearDown() throws Exception {
        sequencer.close();
    }

    @Test
    void test1() throws Exception {
        exec();
    }

    /** */
    void exec() throws Exception {
Debug.println("START: " + mmf);
        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        Sequence sequence = SmafSystem.getSequence(Path.of(mmf).toFile());
        volume(((Synthesizer) sequencer).getReceiver(), volume);
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
        sequencer.start();
        cdl.await();
Debug.println("END: " + mmf);
        sequencer.removeMetaEventListener(mel);
    }

    /**
     *
     * @param args smaf files ...
     */
    public static void main(String[] args) throws Exception {
        PlaySMAF app = new PlaySMAF();
        app.setup();
        for (String arg : args) {
            app.mmf = arg;
            app.exec();
        }
        app.tearDown();
    }
}
