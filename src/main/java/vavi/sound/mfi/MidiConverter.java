/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import javax.sound.midi.InvalidMidiDataException;


/**
 * MidiConverter.
 * <li>javax.sound.midi パッケージにはない。(MFi オリジナル)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030819 nsano initial version <br>
 *          0.01 031212 nsano add toMfiSequence(Sequence, int) <br>
 */
public interface MidiConverter extends MfiDevice {

    /** */
    @Deprecated
    Sequence toMfiSequence(javax.sound.midi.Sequence sequence)
        throws InvalidMidiDataException;

    /** */
    Sequence toMfiSequence(javax.sound.midi.Sequence sequence, int type)
        throws InvalidMidiDataException;

    /** */
    javax.sound.midi.Sequence toMidiSequence(Sequence sequence)
        throws InvalidMfiDataException;
}
