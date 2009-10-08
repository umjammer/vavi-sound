/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.MachineDependMessage;


/**
 * Unknown Vender System exclusive message sequencer.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 051114 nsano initial version <br>
 */
public class UnknownVenderSequencer implements MachineDependSequencer {

    /** */
    public void sequence(MachineDependMessage message)
        throws InvalidMfiDataException {

        MachineDependFunction mdf = new UndefinedFunction();
        mdf.process(message);
    }
}

/* */
