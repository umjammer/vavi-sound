/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.StringUtil;


/**
 * NEC System exclusive message function 0xf1, 0x01 processor.
 * (Vibrato)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030827 nsano initial version <br>
 */
public class Function241_1 implements MachineDependentFunction {

    /**
     * 0xf1, 0x01 Vibrato, length 4
     *
     * @param    message    see below
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

        // 8 vibrato
logger.log(Level.DEBUG, "Vibrato: " + channel + ", " + data[8]);
logger.log(Level.TRACE, "\n" + StringUtil.getDump(data, 32));
    }
}
