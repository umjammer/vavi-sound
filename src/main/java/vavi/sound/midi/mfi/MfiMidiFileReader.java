/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.midi.BasicMidiFileReader;
import vavi.util.Debug;


/**
 * MfiMidiFileReader implemented by vavi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class MfiMidiFileReader extends BasicMidiFileReader {

    /** MFi から変換された MIDI Sequence を取得します。 */
    public Sequence getSequence(InputStream is)
        throws InvalidMidiDataException,
               IOException {

        try {
            if (!is.markSupported()) {
                throw new IOException("mark not supported: " + is);
            }

            is.mark(4); // 4 は MfiSystem#getSequence が 4 bytes で判断できることに依存している

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
}

/* */
