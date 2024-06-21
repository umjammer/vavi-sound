/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mitsubishi;

import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;


/**
 * Mitsubishi System exclusive message function 0x02 processor.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class Function2 implements MachineDependentFunction {

    /**
     * 0x02 Bend Range MFi2 only
     *
     * @param message see below
     * <pre>
     *  0    delta
     *  1    ff
     *  2    ff
     *  3-4  length
     *  5    vendor
     *  6    02
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        int voice = (data[7] & 0xc0) >> 6;          // 0 ~ 3
        int pitchBendRange = data[7] & 0x3f;        // 0 ~ 16
logger.log(Level.DEBUG, String.format("Pitch Bend Range: %02x %02x", voice, pitchBendRange));
    }
}
