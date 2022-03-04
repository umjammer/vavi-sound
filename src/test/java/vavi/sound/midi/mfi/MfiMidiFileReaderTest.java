/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import org.junit.jupiter.api.Test;

import vavi.util.Debug;


/**
 * MfiMidiFileReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class MfiMidiFileReaderTest {

    @Test
    public void test() throws Exception {
        InputStream is = MfiVaviSequenceTest.class.getResourceAsStream("/test.mld");

        CountDownLatch cdl = new CountDownLatch(1);

        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();
        Sequence sequence = MidiSystem.getSequence(is);
Debug.println(sequence);
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(new MetaEventListener() {
            public void meta(MetaMessage meta) {
Debug.println(meta.getType());
                if (meta.getType() == 47) {
                    cdl.countDown();
                }
            }
        });
        sequencer.start();
        cdl.await();
        sequencer.close();
    }

    //----

    /**
     * Play MFi file.
     * @param args [0] filename
     */
    public static void main(String[] args) throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);

        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();
        Sequence sequence = MidiSystem.getSequence(new File(args[0]));
Debug.println(sequence);
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(new MetaEventListener() {
            public void meta(MetaMessage meta) {
Debug.println(meta.getType());
                if (meta.getType() == 47) {
                    cdl.countDown();
                }
            }
        });
        sequencer.start();
        cdl.await();
        sequencer.close();
    }
}

/* */
