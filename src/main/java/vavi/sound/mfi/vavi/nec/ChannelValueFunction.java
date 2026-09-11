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
 * Base of the 0xf1 messages that carry one value for one channel.
 * <p>
 * They all look the same on the wire, only the function nibble and the meaning
 * of the value differ.
 * </p>
 * <pre>
 * 0        delta
 * 1        ff
 * 2        ff
 * 3-4      length
 * 5        vendor
 *
 * 6        level (01 or 02)
 * 7        f1
 * 8        76543210
 *          ~~ ~~~~~
 *          |  | +---- function
 *          |  +------ extra flag
 *          +--------- channel
 *
 * 9        .6543210    value
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
abstract class ChannelValueFunction implements MachineDependentFunction {

    /** 0x01 (MFi 3.0) or 0x02 (MFi 4.0) */
    abstract int getLevel();

    /** the function nibble */
    abstract int getFunction();

    /** for the log */
    abstract String getName();

    @Override
    public String getId() {
        return VENDOR_NEC + "." + getLevel() + "_241_" + getFunction();
    }

    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.channel = (data[8] & 0xc0) >> 6;  // 0 ~ 3
        this.flag    = (data[8] & 0x20) != 0;  //
        this.value   =  data[9] & 0x7f;        // 0 ~ 127

logger.log(Level.DEBUG, getName() + ": " + channel + "ch" + (flag ? " (flagged)" : "") + ", " + value);
    }

    /** channel 0 ~ 3 */
    private int channel;
    /** bit 5 of the function byte, only the MA-7 SendLevel messages ever set it */
    private boolean flag;
    /** 0 ~ 127 */
    private int value;

    /** channel 0 ~ 3 */
    public int getChannel() {
        return channel;
    }

    /** bit 5 of the function byte */
    public boolean isFlag() {
        return flag;
    }

    /** 0 ~ 127 */
    public int getValue() {
        return value;
    }

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    /** */
    public void setValue(int value) {
        this.value = value & 0x7f;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[5];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) getLevel();
        tmp[2] = (byte) 0xf1;
        tmp[3] = (byte) ((channel << 6) | (flag ? 0x20 : 0x00) | getFunction());
        tmp[4] = (byte) value;
        return tmp;
    }
}
