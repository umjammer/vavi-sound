/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.midi.BasicMidiFileReader;
import vavi.util.Debug;


/**
 * MfiMidiFileReader implemented by vavi.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class MfiMidiFileReader extends BasicMidiFileReader {

    /** MFi Ç©ÇÁïœä∑Ç≥ÇÍÇΩ MIDI Sequence ÇéÊìæÇµÇ‹Ç∑ÅB */
    public Sequence getSequence(InputStream is)
        throws InvalidMidiDataException,
               IOException {

        try {
            if (!is.markSupported()) {
                throw new IOException("mark not supported: " + is);
            }

            is.mark(4); // 4 ÇÕ MfiSystem#getSequence Ç™ 4 bytes Ç≈îªífÇ≈Ç´ÇÈÇ±Ç∆Ç…àÀë∂ÇµÇƒÇ¢ÇÈ

            vavi.sound.mfi.Sequence mfiSequence = MfiSystem.getSequence(is);
//Debug.println(mfiSequence);
            return MfiSystem.toMidiSequence(mfiSequence);
        } catch (InvalidMfiDataException e) {
            is.reset();
Debug.println(e);
//Debug.printStackTrace(e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        } catch (MfiUnavailableException e) {
Debug.println(Level.SEVERE, e);
            throw (IOException) new IOException().initCause(e);
        }
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
