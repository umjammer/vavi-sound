/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import vavi.util.Debug;


/**
 * MfiSystemTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class MfiSystemTest {

    @Test
    public void test() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);
        Path inPath = Paths.get(MfiSystemTest.class.getResource("/test.mld").toURI());
        Sequencer sequencer = MfiSystem.getSequencer();
        sequencer.open();
        Sequence sequence = MfiSystem.getSequence(new BufferedInputStream(Files.newInputStream(inPath)));
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(meta -> {
Debug.println(meta.getType());
            if (meta.getType() == 47) {
                cdl.countDown();
            }
        });
        sequencer.start();
        cdl.await();
        sequencer.close();
    }

    //-------------------------------------------------------------------------

    /**
     * Tests this class.
     *
     * usage: java -Djavax.sound.midi.Sequencer="#Real Time Sequencer" MfiSystem mfi_file ...
     */
    public static void main(String[] args) throws Exception {
        Sequencer sequencer = MfiSystem.getSequencer();
        sequencer.open();
        for (int i = 0; i < args.length; i++) {
Debug.println("START: " + args[i]);
            Sequence sequence = MfiSystem.getSequence(new File(args[i]));
            sequencer.setSequence(sequence);
            if (i == args.length - 1) {
                sequencer.addMetaEventListener(meta -> {
Debug.println(meta.getType());
                    if (meta.getType() == 47) {
                        sequencer.close();
                    }
                });
            }
            sequencer.start();
Debug.println("END: " + args[i]);
        }
    }
}

/* */
