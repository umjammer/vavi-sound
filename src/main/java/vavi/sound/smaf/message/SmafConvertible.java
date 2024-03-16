/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import javax.sound.midi.MidiEvent;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;


/**
 * SmafConvertible.
 * <p>
 * currently the implementation class must be a bean.
 * (have a no-argument constructor)
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public interface SmafConvertible {

    /** TODO the implementation method is not good enough, use BeanUtil etc.? */
    SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context) throws InvalidSmafDataException;
}
