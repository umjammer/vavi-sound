/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.File;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import org.junit.Ignore;
import org.junit.Test;

import vavi.sound.mfi.MfiSystem;
import vavi.util.Debug;
import vavi.util.StringUtil;

import static org.junit.Assert.fail;


/**
 * VaviMidiConverterTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@Ignore
public class VaviMidiConverterTest {

    @Test
    public void test() {
        fail("Not yet implemented");
    }

    //-------------------------------------------------------------------------

    /**
     * Tests this class.
     * <pre>
     * usage:
     *  % java VaviMidiConverter -p in_mld_file
     *  % java VaviMidiConverter -c in_mld_file out_mid_file
     * </pre>
     */
    public static void main(String[] args) throws Exception {

        boolean convert = false;
        boolean play = false;

        if (args[0].equals("-c")) {
            convert = true;
        } else if (args[0].equals("-p")) {
            play = true;
        } else {
            throw new IllegalArgumentException(args[0]);
        }

        File file = new File(args[1]);
        vavi.sound.mfi.Sequence mfiSequence = MfiSystem.getSequence(file);
        Sequence midiSequence = MfiSystem.toMidiSequence(mfiSequence);

        Sequencer midiSequencer = MidiSystem.getSequencer();
Debug.println("midiSequencer: " + midiSequencer);
Debug.println("midiSequencer:T: " + midiSequencer.getTransmitter());
Debug.println("midiSequencer:R: " + midiSequencer.getReceiver());
        midiSequencer.open();
        midiSequencer.setSequence(midiSequence);

        if (play) {
            midiSequencer.start();
            while (midiSequencer.isRunning()) {
                try { Thread.sleep(100); } catch (Exception e) {}
            }
            midiSequencer.stop();
        }

        midiSequencer.close();

        if (convert) {
            int ts[] = MidiSystem.getMidiFileTypes(midiSequence);
Debug.println("types: " + ts.length);
            if (ts.length == 0) {
                throw new IllegalArgumentException("no support type");
            }
            for (int i = 0; i < ts.length; i++) {
Debug.println("type: 0x" + StringUtil.toHex2(ts[i]));
            }

            file = new File(args[2]);
            int r = MidiSystem.write(midiSequence, 0, file);
Debug.println("write: " + r);
        }

        System.exit(0);
    }
}

/* */
