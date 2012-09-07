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
 * NEC System exclusive message function 0x01, 0xf1, 0x05 processor.
 * (StreamOff)
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030829 nsano initial version <br>
 */
public class Function1_241_5 implements MachineDependentFunction {

    /**
     * 0x01, 0xf1, 0x05 StreamOff
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
     * 8        76..0101
     *          ~~  ~~~~
     *          |   +------ 0x5
     *          +---------- channel
     * 9        ...43210    stream wave number
     * </pre>
     */
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.channel =      (data[8] & 0xc0) >> 6;      //
        this.streamNumber =  data[9] & 0x1f;            // stream number 0 ~ 31

Debug.println("StreamOff: " + channel + "ch, No." + streamNumber);

        NecSequencer.getAudioEngine().stop(streamNumber);    // TODO channel ???
    }

    /** channel 0 ~ 3 */
    private int channel;
    /** stream number 0 ~ 31 */
    private int streamNumber;

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public void setStreamNumber(int streamNumber) {
        this.streamNumber = streamNumber & 0x1f;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[5];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) 0xf1;
        tmp[3] = (byte) ((channel << 6) | 0x05);
        tmp[4] = (byte) streamNumber;
        return tmp;
    }
}

/* */
