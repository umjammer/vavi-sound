/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.spi;

import javax.sound.midi.InvalidMidiDataException;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.Sequence;


/**
 * MidiConverter.
 * <li>not in javax.sound.midi package (MFi original)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030819 nsano initial version <br>
 *          0.01 031212 nsano add toMfiSequence(Sequence, int) <br>
 */
public interface MfiMidiConverter {

    /** Converts to a MFi sequence from a MIDI sequence. */
    @Deprecated
    Sequence toMfiSequence(javax.sound.midi.Sequence sequence)
        throws InvalidMidiDataException;

    /** Converts to a MFi sequence from a MIDI sequence. */
    Sequence toMfiSequence(javax.sound.midi.Sequence sequence, int type)
        throws InvalidMidiDataException;

    /** Converts to a MIDI sequence from a MFi sequence. */
    javax.sound.midi.Sequence toMidiSequence(Sequence sequence)
        throws InvalidMfiDataException;

    /** is conversion supported for the mfi sequence */
    boolean isFileTypeSupported(Sequence sequence);

    /** is conversion supported for the midi sequence */
    boolean isFileTypeSupported(javax.sound.midi.Sequence sequence);
}
