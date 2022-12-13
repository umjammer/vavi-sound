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

import vavi.sound.smaf.MetaEventListener;
import vavi.sound.smaf.Sequence;
import vavi.sound.smaf.Sequencer;
import vavi.sound.smaf.SmafSystem;
import vavi.sound.smaf.Synthesizer;


/**
 * test smaf.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 090913 nsano initial version <br>
 */
public class PlaySMAF {

    /**
     *
     * @param args smaf files ...
     */
    public static void main(String[] args) throws Exception {
        Sequencer sequencer = SmafSystem.getSequencer();
        sequencer.open();

Synthesizer synthesizer = (Synthesizer) sequencer;
// sf
Soundbank soundbank = synthesizer.getDefaultSoundbank();
//Instrument[] instruments = synthesizer.getAvailableInstruments();
System.err.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
synthesizer.unloadAllInstruments(soundbank);
File file = new File("/Users/nsano/lib/audio/sf2/SGM-V2.01.sf2");
soundbank = MidiSystem.getSoundbank(file);
synthesizer.loadAllInstruments(soundbank);
//instruments = synthesizer.getAvailableInstruments();
System.err.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
// volume (not work ???)
MidiChannel[] channels = synthesizer.getChannels();
double gain = 0.02d;
for (MidiChannel channel : channels) {
 channel.controlChange(7, (int) (gain * 127.0));
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
            Sequence sequence = SmafSystem.getSequence(new File(arg));
            sequencer.setSequence(sequence);
            sequencer.addMetaEventListener(mel);
            sequencer.start();
            countDownLatch.await();
System.err.println("END: " + arg);
            sequencer.removeMetaEventListener(mel);
        }
        sequencer.close();
    }
}

/* */