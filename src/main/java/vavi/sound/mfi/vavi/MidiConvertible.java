/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;


/**
 * MidiConvertible
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030821 nsano initial version <br>
 *          0.01 030826 nsano change method <br>
 */
public interface MidiConvertible {

    /** */
    MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException;
}

/* */
