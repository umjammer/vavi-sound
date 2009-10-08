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
 * NEC System exclusive message function 0x01, 0xf3, 0x03 processor.
 * (MaxGain ê›íË)
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 051113 nsano initial version <br>
 */
public class Function1_243_3 implements MachineDependFunction {

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
    public void process(MachineDependMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.maxGain = data[9];

Debug.println("MaxGain: " + maxGain);
    }

    /** 0 ~ 96 (-96db) */
    private int maxGain = 24; // -12db

    /** 0 ~ 96 (-96db), default 24 */
    public void setMaxGain(int maxGain) {
        this.maxGain = Math.min(maxGain, 96); // TODO minus Ç™í ÇÈÇÊ
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

/* */
