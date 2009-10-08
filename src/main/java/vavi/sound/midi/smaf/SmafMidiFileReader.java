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
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class SmafMidiFileReader extends BasicMidiFileReader {

    /** SMAF ����ϊ����ꂽ MIDI Sequence ���擾���܂��B */
    public Sequence getSequence(InputStream is)
        throws InvalidMidiDataException,
               IOException {

        try {
            if (!is.markSupported()) {
                throw new IOException("mark not supported: " + is);
            }

            is.mark(4); // 4 �� SmafSystem#getSequence �� 4 bytes �Ŕ��f�ł��邱�ƂɈˑ����Ă���

            vavi.sound.smaf.Sequence sequence = SmafSystem.getSequence(is);
//Debug.println(sequence);
            return SmafSystem.toMidiSequence(sequence);
        } catch (InvalidSmafDataException e) {
            is.reset();
Debug.println(e);
//Debug.printStackTrace(e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        } catch (SmafUnavailableException e) {
Debug.println(Level.SEVERE, e);
            throw (IOException) new IOException().initCause(e);
        }
    }
}

/* */
