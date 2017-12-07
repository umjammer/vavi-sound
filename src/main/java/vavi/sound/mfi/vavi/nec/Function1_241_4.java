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
 * NEC System exclusive message function 0x01, 0xf1, 0x04 processor.
 * (StreamSlaveOn)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070423 nsano initial version <br>
 */
public class Function1_241_4 implements MachineDependentFunction {

    /**
     * 0x01, 0xf1, 0x04 StreamSlaveOn
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
     * 7        f1
     * 8        76..0011
     *          ~~  ~~~~
     *          |   +------ 0x3
     *          +---------- channel
     *
     * 9        ...43210    stream wave number
     * 10       .6543210    velocity
     * </pre>
     */
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.channel =      (data[ 8] & 0xc0) >> 6;  //
        this.streamNumber =  data[ 9] & 0x1f;        // stream number 0 ~ 31
        this.velocity =      data[10] & 0x7f;        // 0 ~ 127

Debug.println("StreamSlaveOn: " + channel + "ch, No." + streamNumber + ", velocity=" + velocity);

        NecSequencer.getAudioEngine().start(streamNumber);
    }

    /** channel 0 ~ 3 */
    private int channel;
    /** stream number 0 ~ 31 */
    private int streamNumber;
    /** velocity 0 ~ 127 */
    private int velocity;

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public void setStreamNumber(int streamNumber) {
        this.streamNumber = streamNumber & 0x1f;
    }

    /** */
    public void setVelocity(int velocity) {
        this.velocity = velocity & 0x7f;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[6];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) 0xf1;
        tmp[3] = (byte) ((channel << 6) | 0x04);
        tmp[4] = (byte) streamNumber;
        tmp[5] = (byte) velocity;
        return tmp;
    }
}

/* */
