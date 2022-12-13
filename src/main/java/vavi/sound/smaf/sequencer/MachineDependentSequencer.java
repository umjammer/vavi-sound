/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.sequencer;

import vavi.sound.smaf.InvalidSmafDataException;


/**
 * Sub sequencer for machine dependent system exclusive message.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/09 umjammer initial version <br>
 */
public interface MachineDependentSequencer {

    /** for {@link MachineDependentSequencer} */
    int META_FUNCTION_ID_MACHINE_DEPEND = 0x01;

    /** */
    void sequence()
        throws InvalidSmafDataException;
}

/* */
