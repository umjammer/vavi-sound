/*
 * Copyright (c) 2009 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.util.concurrent.CountDownLatch;

import vavi.sound.smaf.MetaEventListener;
import vavi.sound.smaf.MetaMessage;
import vavi.sound.smaf.Sequence;
import vavi.sound.smaf.Sequencer;
import vavi.sound.smaf.SmafSystem;


/**
 * test samf.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 090913 nsano initial version <br>
 */
public class Test2 {

    /**
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        final Sequencer sequencer = SmafSystem.getSequencer();
        sequencer.open();
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
            Sequence sequence = SmafSystem.getSequence(new File(args[i]));
            sequencer.setSequence(sequence);
            sequencer.addMetaEventListener(mel);
            sequencer.start();
System.err.println("END: " + args[i]);
            countDownLatch.await();
            sequencer.removeMetaEventListener(mel);
        }
        sequencer.close();
    }
}

/* */
