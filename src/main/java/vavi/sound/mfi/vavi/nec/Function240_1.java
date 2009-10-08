/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependFunction;
import vavi.sound.mfi.vavi.track.MachineDependMessage;
import vavi.util.Debug;


/**
 * NEC System exclusive message function 0xf0, 0x01 processor.
 * (FM voice change)
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030827 nsano initial version <br>
 */
public class Function240_1 implements MachineDependFunction {

    /**
     * 0xf0, 0x_1 FM voice change
     *
     * @param    message    see below
     * <pre>
     * 0    delta
     * 1    ff
     * 2    ff
     * 3-4  length
     * 5    vendor
     *
     * 6    f1
     * </pre>
     */
    public void process(MachineDependMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        int channel = (data[7] & 0xc0) >> 6;    // 0 ~ 3
Debug.println("FM voice change: " + channel);
Debug.dump(data);
    }
}

/* */
