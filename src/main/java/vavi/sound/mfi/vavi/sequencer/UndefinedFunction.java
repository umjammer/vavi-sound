/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependFunction;
import vavi.sound.mfi.vavi.track.MachineDependMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;

/**
 * Undefined function processor.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class UndefinedFunction implements MachineDependFunction {

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
    public void process(MachineDependMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        int f1 = data[6] & 0xff;
        int f2 = -1;
        int f3 = -1;
        if (data.length > 8) {
            f3 = data[8] & 0xff;
Debug.println("undefined function: " + StringUtil.toHex2(f1) + " " + StringUtil.toHex2(f2) + " " + StringUtil.toHex2(f3));
        } else if (data.length > 7) {
            f2 = data[7] & 0xff;
Debug.println("undefined function: " + StringUtil.toHex2(f1) + " " + StringUtil.toHex2(f2));
        } else {
Debug.println("undefined function: " + StringUtil.toHex2(f1));
        }
Debug.dump(message.getMessage(), 128);
    }
}

/* */
