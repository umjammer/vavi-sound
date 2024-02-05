/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Undefined function processor.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class UndefinedFunction implements MachineDependentFunction {

    /**
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

        int f1 = data[6] & 0xff;
        int f2 = -1;
        int f3;
        if (data.length > 8) {
            f3 = data[8] & 0xff;
Debug.printf(Level.INFO, "undefined function: %02x %02x %02x\n", f1, f2, f3);
        } else if (data.length > 7) {
            f2 = data[7] & 0xff;
Debug.printf(Level.INFO, "undefined function: %02x %02x\n", f1, f2);
        } else {
Debug.printf(Level.INFO, "undefined function: %02x \n", f1);
        }
Debug.println(Level.INFO, StringUtil.getDump(message.getMessage(), 128));
    }
}
