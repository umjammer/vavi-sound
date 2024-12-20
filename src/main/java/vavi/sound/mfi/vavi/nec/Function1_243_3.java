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


/**
 * NEC System exclusive message function 0x01, 0xf3, 0x03 processor.
 * (MaxGain setting)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051113 nsano initial version <br>
 */
public class Function1_243_3 implements MachineDependentFunction {

    /**
     * 0x01, 0xf3, 0x03 MaxGain Setting
     *
     * @param    message    see below
     * <pre>
     * 0        delta
     * 1        ff
     * 2        ff
     * 3-4      length
     * 5        vendor
     *
     * 6        01
     * 7        f3
     * 8        ....0011
     *              ~~~~
     *              +------ 0x3
     *
     * 9        maxGain 0x00 ~ 0x60 (-48db), default 0x18 (-12db)
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.maxGain = data[9];

logger.log(Level.DEBUG, "MaxGain: " + maxGain);
    }

    /** 0 ~ 96 (-96db) */
    private int maxGain = 24; // -12db

    /** 0 ~ 96 (-96db), default 24 */
    public void setMaxGain(int maxGain) {
        this.maxGain = Math.min(maxGain, 96); // TODO minus accepted
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[5];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) 0xf3;
        tmp[3] = (byte) 0x03;
        tmp[4] = (byte) maxGain;
        return tmp;
    }
}
