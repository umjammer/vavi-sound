/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;

import static vavi.sound.mfi.vavi.nec.NecSequencer.VENDOR_NEC;


/**
 * Base of the 0xf1 messages that are a switch for one channel and carry no data
 * at all - the whole message is four bytes.
 * <pre>
 * 0        delta
 * 1        ff
 * 2        ff
 * 3-4      length (0004)
 * 5        vendor
 *
 * 6        01
 * 7        f1
 * 8        76...nnn
 *          ~~   ~~~
 *          |    +----- function
 *          +---------- channel
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
abstract class ChannelFlagFunction implements MachineDependentFunction {

    /** the function nibble */
    abstract int getFunction();

    /** for the log */
    abstract String getName();

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "1_241_" + getFunction();
    }

    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.channel = (data[8] & 0xc0) >> 6;    // 0 ~ 3

logger.log(Level.DEBUG, getName() + ": " + channel + "ch");
    }

    /** channel 0 ~ 3 */
    private int channel;

    /** channel 0 ~ 3 */
    public int getChannel() {
        return channel;
    }

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[4];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) 0xf1;
        tmp[3] = (byte) ((channel << 6) | getFunction());
        return tmp;
    }
}
