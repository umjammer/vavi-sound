/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;


/**
 * NEC System exclusive message function 0x01, 0xf3, 0x04 processor.
 * (ストリーム数指定)
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 051113 nsano initial version <br>
 */
public class Function1_243_4 implements MachineDependentFunction {

    /**
     * 0x01, 0xf3, 0x04 Stream Number Setting
     *
     * @param message see below
     * <pre>
     * 0        delta
     * 1        ff
     * 2        ff
     * 3-4      length
     * 5        vendor
     *
     * 6        01
     * 7        f3
     * 8        ....0100
     *              ~~~~
     *              +------ 0x4
     * 9        ......00
     *                ~~
     *                +---- max stream number, 0 ~ 2, default 0
     * </pre>
     */
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.maxStreamNumber = data[9] & 0x03;

Debug.println("maxStreamNumber: " + maxStreamNumber);
    }

    /** 0 ~ 2, default 0 */
    private int maxStreamNumber;

    /** */
    public void setMaxStreamNumber(int streamNumber) {
        this.maxStreamNumber = streamNumber & 0x03;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[5];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) 0xf3;
        tmp[3] = (byte) 0x04;
        tmp[4] = (byte) maxStreamNumber;
        return tmp;
    }
}

/* */
