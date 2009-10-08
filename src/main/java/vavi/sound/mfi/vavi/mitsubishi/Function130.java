/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mitsubishi;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependFunction;
import vavi.sound.mfi.vavi.track.MachineDependMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Mitsubishi System exclusive message function 0x82 processor.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class Function130 implements MachineDependFunction {

    /**
     * 0x82 ADPCM pan MFi2, MFi3
     *
     * @param message see below
     * <pre>
     *  0   delta
     *  1   ff
     *  2   ff
     *  3-4 length
     *  5   vendor
     *  6   82
     * </pre>
     */
    public void process(MachineDependMessage message)
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
        tmp[0] = (byte) (VENDOR_MITSUBISHI | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x82;
        tmp[2] = (byte) ((channel << 6) | panpot);
        return tmp;
    }
}

/* */
