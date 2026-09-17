/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.spi;

import javax.sound.midi.InvalidMidiDataException;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.Sequence;


/**
 * MidiConverter.
 * <li>not in javax.sound.midi package (SMAF original)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260912 nsano initial version <br>
 */
public interface SmafMidiConverter {

    /** Converts to a SMAF sequence from a MIDI sequence. */
    @Deprecated
    Sequence toSmafSequence(javax.sound.midi.Sequence sequence)
        throws InvalidMidiDataException;

    /** Converts to a SMAF sequence from a MIDI sequence. */
    Sequence toSmafSequence(javax.sound.midi.Sequence sequence, int type)
        throws InvalidMidiDataException;

    /** Converts to a MIDI sequence from a SMAF sequence. */
    javax.sound.midi.Sequence toMidiSequence(Sequence sequence)
        throws InvalidSmafDataException;

    /** is conversion supported for the mfi sequence */
    boolean isFileTypeSupported(Sequence sequence);

    /** is conversion supported for the midi sequence */
    boolean isFileTypeSupported(javax.sound.midi.Sequence sequence);
}
