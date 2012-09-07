/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;


/**
 * NEC System exclusive message function 0x01, 0xf1, 0x06 processor.
 * (StreamPan)
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 070423 nsano initial version <br>
 */
public class Function1_241_6 implements MachineDependentFunction {

    /**
     * 0x01, 0xf1, 0x06 StreamPan
     *
     * @param message see below
     * <pre>
     * 0        delta
     * 1        ff
     * 2        ff
     * 3-4      length
     * 5        vendor
     * 6        01
     * 7        f1
     * 8        76..0110
     *          ~~  ~~~~
     *          |   +------ 0x6
     *          +---------- channel
     * 9        ...43210    stream wave number
     * 10       0 ~ 63, 128 mono, 255 no pan
     * </pre>
     */
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.channel =      (data[ 8] & 0xc0) >> 6; //
        this.streamNumber =  data[ 9] & 0x1f;       // stream number 0 ~ 31
        this.pan          =  data[10];              // pan

Debug.println("StreamPan: " + channel + "ch, No." + streamNumber + ", pan: " + pan);
    }

    /** channel 0 ~ 3 */
    private int channel;
    /** stream number 0 ~ 31 */
    private int streamNumber;

    /** pan 0 ~ 63, 128 mono, 255 no pan */
    private int pan;

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public void setStreamNumber(int streamNumber) {
        this.streamNumber = streamNumber & 0x1f;
    }

    /** @param pan 0 ~ 63, 128 mono, 255 no pan */
    public void setPan(int pan) {
        this.pan = pan;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[6];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) 0xf1;
        tmp[3] = (byte) ((channel << 6) | 0x06);
        tmp[4] = (byte) streamNumber;
        tmp[5] = (byte) pan;
        return tmp;
    }
}

/* */
