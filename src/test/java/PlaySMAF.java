/*
 * Copyright (c) 2009 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;

import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.MetaEventListener;
import vavi.sound.smaf.Sequence;
import vavi.sound.smaf.Sequencer;
import vavi.sound.smaf.SmafSystem;
import vavi.sound.smaf.Synthesizer;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * test smaf.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 090913 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class PlaySMAF {

    /**
     *
     * @param args smaf files ...
     */
    public static void main(String[] args) throws Exception {
        PlaySMAF app = new PlaySMAF();
        PropsEntity.Util.bind(app);
        app.exec(args);
    }

    static final float volume = Float.parseFloat(System.getProperty("vavi.test.volume",  "0.2"));

    @Property(name = "sf2")
    String sf2 = System.getProperty("user.home") + "/Library/Audio/Sounds/Banks/Orchestra/default.sf2";

    /** */
    void exec(String[] args) throws Exception {
        Sequencer sequencer = SmafSystem.getSequencer();
        sequencer.open();

Synthesizer synthesizer = (Synthesizer) sequencer;
// sf
Soundbank soundbank = synthesizer.getDefaultSoundbank();
//Instrument[] instruments = synthesizer.getAvailableInstruments();
System.err.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
synthesizer.unloadAllInstruments(soundbank);
File file = new File(sf2);
soundbank = MidiSystem.getSoundbank(file);
synthesizer.loadAllInstruments(soundbank);
//instruments = synthesizer.getAvailableInstruments();
System.err.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
// volume (not work ???)
MidiChannel[] channels = synthesizer.getChannels();

        for (String arg : args) {
System.err.println("START: " + arg);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            MetaEventListener mel = meta -> {
System.err.println("META: " + meta.getType());
                if (meta.getType() == 47) {
                    countDownLatch.countDown();
                }
            };
            Sequence sequence = SmafSystem.getSequence(new File(arg));
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
