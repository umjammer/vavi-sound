/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */


import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

import vavi.sound.midi.MidiUtil;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * sound font.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080701 nsano initial version <br>
 * @see "https://stackoverflow.com/a/45119638/6102938"
 */
@PropsEntity(url = "file:local.properties")
public class SoundFontTest {

    /**
     * @param args 0: midi
     */
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);

        SoundFontTest app = new SoundFontTest();
        PropsEntity.Util.bind(app);
        app.exec(file);
    }

    static final float volume = Float.parseFloat(System.getProperty("vavi.test.volume",  "0.2"));

    @Property(name = "sf2")
    String sf2name = System.getProperty("user.home") + "/Library/Audio/Sounds/Banks/Orchestra/default.sf2";

    /** */
    void exec(File file) throws Exception {
        Sequence sequence = MidiSystem.getSequence(file);
Debug.println(Level.FINE, "sequence: " + sequence);

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println(Level.FINE, "synthesizer: " + synthesizer);
        synthesizer.open();

        // sf
        Soundbank soundbank = synthesizer.getDefaultSoundbank();
//        Instrument[] instruments = synthesizer.getAvailableInstruments();
//System.err.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
        synthesizer.unloadAllInstruments(soundbank);
        File sf2 = new File(sf2name);
        soundbank = MidiSystem.getSoundbank(sf2);
        synthesizer.loadAllInstruments(soundbank);
//        instruments = synthesizer.getAvailableInstruments();
Debug.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
Debug.println("META: " + meta.getType());
            if (meta.getType() == 47) {
                countDownLatch.countDown();
            }
        };
        Sequencer sequencer = MidiSystem.getSequencer(false); // crux
Debug.println(Level.FINE, "sequencer: " + sequencer);
        sequencer.open();
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());

        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
        sequencer.start();
MidiUtil.volume(synthesizer.getReceiver(), volume);
        countDownLatch.await();
        sequencer.stop();
        sequencer.removeMetaEventListener(mel);
        sequencer.close();
    }
}
