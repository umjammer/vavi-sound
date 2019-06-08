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

import vavi.sound.mfi.MetaEventListener;
import vavi.sound.mfi.MetaMessage;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.Sequencer;
import vavi.sound.mfi.Synthesizer;


/**
 * test mfi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 090913 nsano initial version <br>
 */
public class Test1 {

    /**
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        final Sequencer sequencer = MfiSystem.getSequencer();
        sequencer.open();
Synthesizer synthesizer = Synthesizer.class.cast(sequencer);
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
for (int i = 0; i < channels.length; i++) {
 channels[i].controlChange(7, (int) (gain * 127.0));
}

        for (int i = 0; i < args.length; i++) {
System.err.println("START: " + args[i]);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            MetaEventListener mel = new MetaEventListener() {
                public void meta(MetaMessage meta) {
System.err.println("META: " + meta.getType());
                    if (meta.getType() == 47) {
                        countDownLatch.countDown();
                    }
                }
            };
            Sequence sequence = MfiSystem.getSequence(new File(args[i]));
            sequencer.setSequence(sequence);
            sequencer.addMetaEventListener(mel);
            sequencer.start();
            countDownLatch.await();
System.err.println("END: " + args[i]);
            sequencer.removeMetaEventListener(mel);
        }
        sequencer.close();
    }
}

/* */
