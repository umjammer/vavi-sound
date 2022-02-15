/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.io.File;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

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

        Sequencer sequencer = MidiSystem.getSequencer();
Debug.println("sequencer: " + sequencer);
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
