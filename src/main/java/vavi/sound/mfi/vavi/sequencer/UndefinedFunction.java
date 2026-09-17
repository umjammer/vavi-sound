/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.lang.System.Logger.Level;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.util.StringUtil;


/**
 * Undefined function processor.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class UndefinedFunction implements MachineDependentFunction {

    @Override
    public String getId() {
        return null;
    }

    /**
     *
     * @param data     see below
     *                 <pre>
     *                 0    delta
     *                 1    ff
     *                 2    ff
     *                 3-4  length
     *                 5    vendor
     *                 6    f1
     *                 </pre>
     * @param receiver
     */
    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        int f1 = data[6] & 0xff;
        int f2 = 0;
        int f3;
        if (data.length > 8) {
            f3 = data[8] & 0xff;
logger.log(Level.INFO, "undefined function: %02x %02x %02x".formatted(f1, f2, f3) + "\n" + StringUtil.getDump(data, 128));
        } else if (data.length > 7) {
            f2 = data[7] & 0xff;
logger.log(Level.INFO, "undefined function: %02x %02x".formatted(f1, f2) + "\n" + StringUtil.getDump(data, 128));
        } else {
logger.log(Level.INFO, "undefined function: %02x".formatted(f1) + "\n" + StringUtil.getDump(data, 128));
        }
    }
}
