/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sharp;

import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;


/**
 * Sharp System exclusive message function 0x81 processor.
 * (Wave Channel Volume)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051111 nsano initial version <br>
 */
public class Function129 implements MachineDependentFunction {

    /**
     * 0x81 Wave Channel Volume
     *
     * @param message see below
     * <pre>
     *  0    delta
     *  1    ff
     *  2    ff
     *  3-4  length
     *  5    vendor
     *  6    0x81
     *  7    76 543210
     *       ~~ ~~~~~~
     *       |  +- volume
     *       +- channel
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.channel = (data[7] & 0xc0) >> 6;   // 0 ~ 3
        this.volume  =  data[7] & 0x3f;         //
logger.log(Level.DEBUG, "ADPCM volume: %dch %02x".formatted(channel, volume));
    }

    /** */
    private int channel;
    /** */
    private int volume = 63;

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public void setVolume(int volume) {
        this.volume = volume & 0x3f;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[3];
        tmp[0] = (byte) (VENDOR_SHARP | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x81;
        tmp[2] = (byte) ((channel << 6) | volume);
        return tmp;
    }
}
