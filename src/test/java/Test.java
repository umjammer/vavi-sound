/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */


import java.io.File;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

import vavi.util.Debug;


/**
 * Test.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080701 nsano initial version <br>
 */
public class Test {

    /**
     * @param args 0: midi
     */
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);

        Sequence sequence = MidiSystem.getSequence(file);
Debug.println("sequence: " + sequence);

        Sequencer sequencer = MidiSystem.getSequencer(true);
Debug.println("sequencer: " + sequencer);

        Synthesizer synthesizer = MidiSystem.getSynthesizer();
Debug.println("synthesizer: " + synthesizer);
        // sf
        Soundbank soundbank = synthesizer.getDefaultSoundbank();
//        Instrument[] instruments = synthesizer.getAvailableInstruments();
//System.err.println("---- " + soundbank.getDescription() + " ----");
//Arrays.asList(instruments).forEach(System.err::println);
        synthesizer.unloadAllInstruments(soundbank);
        File sf2 = new File("/Users/nsano/lib/audio/sf2/SGM-V2.01.sf2");
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

        sequencer.open();
        sequencer.setSequence(sequence);
        sequencer.start();
        while (sequencer.isRunning()) {
            try { Thread.sleep(100); } catch (Exception e) {}
        }
        sequencer.stop();
        sequencer.close();
    }
}

/* */
