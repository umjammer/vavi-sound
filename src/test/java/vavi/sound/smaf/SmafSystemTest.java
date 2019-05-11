/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * SmafSystemTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
@Disabled
public class SmafSystemTest {

    @Test
    public void test() {
        fail("Not yet implemented");
    }

    //-------------------------------------------------------------------------

    /**
     * Tests this class.
     * <pre>
     * usage:
     *  % java -Djavax.sound.midi.Sequencer="#Java MIDI(MFi/SMAF) ADPCM Sequencer" SmafSystem -p in_mmf_file
     *  % java SmafSystem -c in_mmf_file out_mid_file
     * </pre>
     */
    public static void main(String[] args) throws Exception {
//try {
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
        vavi.sound.smaf.Sequence smafSequence = SmafSystem.getSequence(new BufferedInputStream(new FileInputStream(file)));
        Sequence midiSequence = SmafSystem.toMidiSequence(smafSequence);

        if (play) {
            javax.sound.midi.Sequencer midiSequencer = MidiSystem.getSequencer();
            midiSequencer.open();
            midiSequencer.setSequence(midiSequence);

            midiSequencer.start();
            while (midiSequencer.isRunning()) {
                try { Thread.sleep(100); } catch (Exception e) {}
            }
            midiSequencer.stop();

            midiSequencer.close();
        }

        if (convert) {
//Debug.println("☆☆☆ here: " + midiSequence);
            int ts[] = MidiSystem.getMidiFileTypes(midiSequence);
//Debug.println("★★★ here");
//Debug.println("types: " + ts.length);
            if (ts.length == 0) {
                throw new IllegalArgumentException("no support type");
            }
            for (int i = 0; i < ts.length; i++) {
//Debug.println("type: 0x" + StringUtil.toHex2(ts[i]));
            }

            file = new File(args[2]);
            int r = MidiSystem.write(midiSequence, 0, file);
Debug.println("write: " + r + " bytes as '" + args[2] + "'");
        }

        System.exit(0);
//} catch (Throwable t) {
// Debug.printStackTrace(t);
// System.exit(1);
//}
    }
}

/* */
