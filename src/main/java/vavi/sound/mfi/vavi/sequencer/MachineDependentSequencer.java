/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.properties.PrefixedPropertiesFactory;


/**
 * Sub sequencer for machine dependent system exclusive message.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020704 nsano initial version <br>
 */
public interface MachineDependentSequencer {

    /** for {@link MachineDependentSequencer} */
    static final int META_FUNCTION_ID_MACHINE_DEPEND = 0x01;

    /** */
    void sequence(MachineDependentMessage message)
        throws InvalidMfiDataException;

    /** */
    static final PrefixedPropertiesFactory<Integer, MachineDependentSequencer> factory =
        new PrefixedPropertiesFactory<>("/vavi/sound/mfi/vavi/vavi.properties", "sequencer.vendor.");
}

/* */
