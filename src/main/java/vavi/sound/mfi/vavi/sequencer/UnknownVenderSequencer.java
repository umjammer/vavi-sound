/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;


/**
 * Unknown Vender System exclusive message sequencer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051114 nsano initial version <br>
 */
public class UnknownVenderSequencer implements MachineDependentSequencer {

    @Override
    public void sequence(MachineDependentMessage message)
        throws InvalidMfiDataException {

        MachineDependentFunction mdf = new UndefinedFunction();
        mdf.process(message);
    }
}

/* */
