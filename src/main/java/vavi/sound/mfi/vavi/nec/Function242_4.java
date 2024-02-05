/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * NEC System exclusive message function 0xf2, 0x04 processor.
 * (vibrator)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030827 nsano initial version <br>
 */
public class Function242_4 implements MachineDependentFunction {

    /**
     * 0xf2, 0x04 Vibrator, length 5
     *
     * @param message see below
     * <pre>
     * 0    delta
     * 1    ff
     * 2    ff
     * 3-4  length
     * 5    vendor
     * 6    f1
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        int channel = (data[7] & 0xc0) >> 6;    // 0 ~ 3

        // 8    ?
        // 9    ?    00
Debug.println(Level.FINE, "Vibrator: " + channel);
Debug.println(Level.FINEST, StringUtil.getDump(data));
    }
}
