/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sharp;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Sharp System exclusive message function 0x82 processor.
 * (Wave Channel Panpot)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051111 nsano initial version <br>
 */
public class Function130 implements MachineDependentFunction {

    /**
     * 0x82 Wave Channel Panpot
     *
     * @param message see below
     * <pre>
     *  0    delta
     *  1    ff
     *  2    ff
     *  3-4  length
     *  5    vendor
     *  6    0x82
     *  7    76 543210
     *       ~~ ~~~~~~
     *       |  +- pan
     *       +- channel
     * </pre>
     */
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.channel = (data[7] & 0xc0) >> 6;   // 0 ~ 3
        this.panpot  =  data[7] & 0x3f;         // 0x20 center
Debug.println("ADPCM pan: " + channel + "ch " + StringUtil.toHex2(panpot));
    }

    /** */
    private int channel;
    /** */
    private int panpot = 32;

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public void setPanpot(int panpot) {
        this.panpot = panpot & 0x3f;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[3];
        tmp[0] = (byte) (VENDOR_SHARP | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x82;
        tmp[2] = (byte) ((channel << 6) | panpot);
        return tmp;
    }
}

/* */
