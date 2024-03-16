/*
 * Copyright (c) 2009 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;

import org.junit.jupiter.api.BeforeEach;
import vavi.sound.mfi.MetaEventListener;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.Sequencer;
import vavi.sound.mfi.Synthesizer;
import vavi.sound.midi.MidiUtil;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * test mfi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 090913 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class PlayMFi {

    /**
     *
     * @param args mfi files ...
     */
    public static void main(String[] args) throws Exception {
        PlayMFi app = new PlayMFi();
        PropsEntity.Util.bind(app);
        app.exec(args);
    }

    static final float volume = Float.parseFloat(System.getProperty("vavi.test.volume",  "0.2"));

    @Property(name = "sf2")
    String sf2 = System.getProperty("user.home") + "/Library/Audio/Sounds/Banks/Orchestra/default.sf2";

    /** */
    void exec(String[] args) throws Exception {
        Sequencer sequencer = MfiSystem.getSequencer();
        sequencer.open();
Synthesizer synthesizer = (Synthesizer) sequencer;
// sf
File file = new File(sf2);
 if (file.exists()) {
 Soundbank soundbank = synthesizer.getDefaultSoundbank();
//Instrument[] instruments = synthesizer.getAvailableInstruments();
 System.err.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
 synthesizer.unloadAllInstruments(soundbank);
 soundbank = MidiSystem.getSoundbank(file);
 synthesizer.loadAllInstruments(soundbank);
//instruments = synthesizer.getAvailableInstruments();
 System.err.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
}
        for (String arg : args) {
System.err.println("START: " + arg);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            MetaEventListener mel = meta -> {
System.err.println("META: " + meta.getType());
                if (meta.getType() == 47) {
                    countDownLatch.countDown();
                }
            };
            Sequence sequence = MfiSystem.getSequence(new File(arg));
            sequencer.setSequence(sequence);
            sequencer.addMetaEventListener(mel);
            sequencer.start();
MidiUtil.volume(synthesizer.getReceiver(), volume); // TODO noise
            countDownLatch.await();
System.err.println("END: " + arg);
            sequencer.removeMetaEventListener(mel);
        }
        sequencer.close();
    }
}
