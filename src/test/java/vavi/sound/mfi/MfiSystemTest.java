/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import java.io.File;

import org.junit.Test;

import vavi.util.Debug;

import static org.junit.Assert.*;


/**
 * MfiSystemTest. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class MfiSystemTest {

    @Test
    public void test() {
        fail("Not yet implemented");
    }

    //-------------------------------------------------------------------------

    /**
     * Tests this class.
     *
     * usage: java -Djavax.sound.midi.Sequencer="#Real Time Sequencer" MfiSystem mfi_file ...
     */
    public static void main(String[] args) throws Exception {
        final Sequencer sequencer = MfiSystem.getSequencer();
        sequencer.open();
        for (int i = 0; i < args.length; i++) {
Debug.println("START: " + args[i]);
            Sequence sequence = MfiSystem.getSequence(new File(args[i]));
            sequencer.setSequence(sequence);
            if (i == args.length - 1) {
                sequencer.addMetaEventListener(new MetaEventListener() {
                    public void meta(MetaMessage meta) {
Debug.println(meta.getType());
                        if (meta.getType() == 47) {
                            sequencer.close();
                        }
                    }
                });
            }
            sequencer.start();
Debug.println("END: " + args[i]);
        }
    }
}

/* */
