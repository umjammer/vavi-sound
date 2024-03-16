/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.util.properties.PrefixedClassPropertiesFactory;
import vavi.util.properties.PrefixedPropertiesFactory;


/**
 * MfiConvertible
 * <p>
 * Currently, an implementation class of this interface should be an bean.
 * (means having a contractor without argument)
 * </p>
 * <pre>
 * properties file ... "/vavi/sound/mfi/vavi/vavi.properties"
 * name prefix ... "midi."
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030905 nsano initial version <br>
 */
public interface MfiConvertible {

    /** */
    MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException;

    /** factory */
    PrefixedPropertiesFactory<String, MfiConvertible> factory =
        new PrefixedClassPropertiesFactory<>("/vavi/sound/mfi/vavi/vavi.properties", "midi.");
}
