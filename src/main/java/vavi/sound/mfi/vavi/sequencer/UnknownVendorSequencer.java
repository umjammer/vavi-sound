/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;


/**
 * Unknown Vendor System exclusive message sequencer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051114 nsano initial version <br>
 */
public class UnknownVendorSequencer implements MachineDependentSequencer {

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public void sequence(MachineDependentMessage message, Receiver receiver)
        throws InvalidMfiDataException {

        MachineDependentFunction mdf = new UndefinedFunction();
        mdf.process(message, receiver);
    }
}
