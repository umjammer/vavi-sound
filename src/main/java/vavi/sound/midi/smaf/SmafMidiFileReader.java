/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.midi.BasicMidiFileReader;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafSystem;
import vavi.sound.smaf.SmafUnavailableException;
import vavi.util.Debug;


/**
 * SmafMidiFileReader implemented by vavi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class SmafMidiFileReader extends BasicMidiFileReader {

    /** SMAF から変換された MIDI Sequence を取得します。 */
    public Sequence getSequence(InputStream is)
        throws InvalidMidiDataException,
               IOException {

        try {
            if (!is.markSupported()) {
                throw new IOException("mark not supported: " + is);
            }

            is.mark(4); // 4 は SmafSystem#getSequence が 4 bytes で判断できることに依存している

            vavi.sound.smaf.Sequence sequence = SmafSystem.getSequence(is);
//Debug.println(sequence);
            return SmafSystem.toMidiSequence(sequence);
        } catch (InvalidSmafDataException e) {
            is.reset();
Debug.println(Level.FINE, e);
//Debug.printStackTrace(e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        } catch (SmafUnavailableException e) {
Debug.println(Level.SEVERE, e);
            throw (IOException) new IOException().initCause(e);
        }
    }
}

/* */
