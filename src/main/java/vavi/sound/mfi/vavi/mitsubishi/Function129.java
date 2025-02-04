/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mitsubishi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;

import static java.lang.System.getLogger;


/**
 * Mitsubishi System exclusive message function 0x81 processor.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class Function129 implements MachineDependentFunction {

    private static final Logger logger = getLogger(Function129.class.getName());

    /**
     * 0x81 ADPCM volume MFi2, MFi3
     *
     * @param message see below
     * <pre>
     *  0   delta
     *  1   ff
     *  2   ff
     *  3-4 length
     *  5   vendor
     *  6   81
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.channel = (data[7] & 0xc0) >> 6;   // 0 ~ 3
        this.volume  =  data[7] & 0x3f;         //
logger.log(Level.DEBUG, "ADPCM volume: %dch %02x\n".formatted(channel, volume));
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
        tmp[0] = (byte) (VENDOR_MITSUBISHI | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x81;
        tmp[2] = (byte) ((channel << 6) | volume);
        return tmp;
    }
}
