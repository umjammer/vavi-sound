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
import vavi.util.StringUtil;


/**
 * NEC System exclusive message function 0xf1, 0x_0 processor.
 * (ADPCM on)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030827 nsano initial version <br>
 */
public class Function241_0 implements MachineDependentFunction {

    /**
     * 0xf1, 0x_0 ADPCM on, length 5
     *
     * @param message see below
     * <pre>
     * 0    delta
     * 1    ff
     * 2    ff
     * 3-4  length
     * 5    vendor
     * 6    f1
     * 7    _0
     * 8    streamNumber
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();
logger.log(Level.TRACE, "data:\n" + StringUtil.getDump(data, 64));

        this.channel      = (data[7] & 0xc0) >> 6;  // 0 ~ 3
        this.streamNumber =  data[8] & 0xff;        // stream number 0 ~ 31

        NecSequencer.getAudioEngine().start(streamNumber);
    }

    /** */
    private int channel;
    /** */
    private int streamNumber;

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public void setStreamNumber(int streamNumber) {
        this.streamNumber = streamNumber;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[4];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0xf1;                           // f1
        tmp[2] = (byte) ((channel << 6) | 0x0);         // f2 & channel
        tmp[3] = (byte) streamNumber;
        return tmp;
    }
}
