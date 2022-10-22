/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */


import java.io.File;
import java.util.concurrent.CountDownLatch;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

import vavi.util.Debug;


/**
 * sound font.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080701 nsano initial version <br>
 * @see "https://stackoverflow.com/a/45119638/6102938"
 */
public class Test {

    /**
     * @param args 0: midi
     */
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);

        Sequence sequence = MidiSystem.getSequence(file);
Debug.println("sequence: " + sequence);

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println("synthesizer: " + synthesizer);
        synthesizer.open();

        // sf
        Soundbank soundbank = synthesizer.getDefaultSoundbank();
//        Instrument[] instruments = synthesizer.getAvailableInstruments();
//System.err.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
        synthesizer.unloadAllInstruments(soundbank);
//        String sf2name = "SGM-V2.01.sf2";
        String sf2name = "Aspirin-Stereo.sf2";
        File sf2 = new File("/Users/nsano/lib/audio/sf2", sf2name);
        soundbank = MidiSystem.getSoundbank(sf2);
        synthesizer.loadAllInstruments(soundbank);
//        instruments = synthesizer.getAvailableInstruments();
System.err.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);

        // volume (not work ???)
//        MidiChannel[] channels = synthesizer.getChannels();
//        double gain = 0.02d;
//        for (int i = 0; i < channels.length; i++) {
//            channels[i].controlChange(7, (int) (gain * 127.0));
//        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MetaEventListener mel = meta -> {
System.err.println("META: " + meta.getType());
            if (meta.getType() == 47) {
                countDownLatch.countDown();
            }
        };
        Sequencer sequencer = MidiSystem.getSequencer(false); // crux
Debug.println("sequencer: " + sequencer);
        sequencer.open();
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());

        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(mel);
        sequencer.start();
        countDownLatch.await();
        sequencer.stop();
        sequencer.removeMetaEventListener(mel);
        sequencer.close();
    }
}

/* */
