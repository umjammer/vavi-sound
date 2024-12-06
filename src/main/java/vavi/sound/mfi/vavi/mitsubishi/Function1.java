/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mitsubishi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;

import static java.lang.System.getLogger;


/**
 * Mitsubishi System exclusive message function 0x01 processor.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class Function1 implements MachineDependentFunction {

    private static final Logger logger = getLogger(Function1.class.getName());

    /**
     * 0x01 Pitch Bend MFi2 only
     *
     * @param message see below
     * <pre>
     * 0    delta
     * 1    ff
     * 2    ff
     * 3-4  length
     * 5    vendor
     * 6    01
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        int voice     = (data[7] & 0xc0) >> 6;  // 0 ~ 3
        int pitchBend =  data[7] & 0x3f;        // -32 ~ 31
        pitchBend *= (data[7] & 0x20) != 0 ? -1 : 0;
        // pitchBend * Pitch Bend Range * 100 / 32 [cent]
logger.log(Level.DEBUG, "Pitch Bend: %02x %d".formatted(voice, pitchBend));
    }
}
