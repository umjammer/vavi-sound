/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.properties.PrefixedClassPropertiesFactory;
import vavi.util.properties.PrefixedPropertiesFactory;


/**
 * Sub sequencer for machine dependent system exclusive message.
 * <pre>
 * properties file ... "/vavi/sound/mfi/vavi/vavi.properties"
 * name prefix ... "sequencer.vendor."
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020704 nsano initial version <br>
 */
public interface MachineDependentSequencer {

    /** for {@link MachineDependentSequencer} */
    int META_FUNCTION_ID_MACHINE_DEPEND = 0x01;

    /** processes a message */
    void sequence(MachineDependentMessage message)
        throws InvalidMfiDataException;

    /** factory */
    PrefixedPropertiesFactory<Integer, MachineDependentSequencer> factory =
        new PrefixedClassPropertiesFactory<>("/vavi/sound/mfi/vavi/vavi.properties", "sequencer.vendor.");
}
