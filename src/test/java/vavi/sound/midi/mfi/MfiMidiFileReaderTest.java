/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.File;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * MfiMidiFileReaderTest. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@Disabled
public class MfiMidiFileReaderTest {

    @Test
    public void test() {
        fail("Not yet implemented");
    }

    //----

    /** 
     * Play MFi file.
     * @param args [0] filename
     */
    public static void main(String[] args) throws Exception {
        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();
        Sequence sequence = MidiSystem.getSequence(new File(args[0]));
Debug.println(sequence);
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(new MetaEventListener() {
            public void meta(MetaMessage meta) {
Debug.println(meta.getType());
                if (meta.getType() == 47) {
                    System.exit(0);
                }
            }
        });
        sequencer.start();
    }
}

/* */
