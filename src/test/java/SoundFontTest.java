/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

import org.junit.jupiter.api.BeforeEach;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.midi.MidiUtil.volume;


/**
 * sound font.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080701 nsano initial version <br>
 * @see "https://stackoverflow.com/a/45119638/6102938"
 */
@PropsEntity(url = "file:local.properties")
public class SoundFontTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume.midi")
    float volume = 0.2f;

    @Property(name = "sf2")
    String sf2name = System.getProperty("user.home") + "/Library/Audio/Sounds/Banks/Orchestra/default.sf2";

    @Property
    String file;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
Debug.println("volume: " + volume);
    }

    /** */
    void exec() throws Exception {
        Path path = Path.of(file);
Debug.println("file: " + file);
        Sequence sequence = MidiSystem.getSequence(path.toFile());
Debug.println(Level.FINE, "sequence: " + sequence);

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println(Level.FINE, "synthesizer: " + synthesizer);
        synthesizer.open();

        // sf
        Soundbank soundbank = synthesizer.getDefaultSoundbank();
//        Instrument[] instruments = synthesizer.getAvailableInstruments();
//Debug.print("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
        synthesizer.unloadAllInstruments(soundbank);
        File sf2 = new File(sf2name);
        soundbank = MidiSystem.getSoundbank(sf2);
        synthesizer.loadAllInstruments(soundbank);
//        instruments = synthesizer.getAvailableInstruments();
Debug.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);

        CountDownLatch cdl = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) cdl.countDown();
        };
        Sequencer sequencer = MidiSystem.getSequencer(false); // crux
Debug.println(Level.FINE, "sequencer: " + sequencer);
        sequencer.open();
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());

        volume(synthesizer.getReceiver(), volume);
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
        sequencer.start();
        cdl.await();
        sequencer.stop();
        sequencer.removeMetaEventListener(mel);
        sequencer.close();
    }

    /**
     * @param args 0: midi
     */
    public static void main(String[] args) throws Exception {
        SoundFontTest app = new SoundFontTest();
        app.setup();
        app.file = args[0];
        app.exec();
    }
}
