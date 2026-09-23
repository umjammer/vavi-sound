/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.fujitsu;

import java.lang.System.Logger.Level;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;


/**
 * Base of the Fujitsu MFi 4.0 messages that set one numbered parameter
 * (0x93, 0xb0, 0xb1).
 * <p>
 * They are all exactly 7 bytes and share one payload shape:
 * </p>
 * <pre>
 * 0     delta
 * 1     ff
 * 2     ff
 * 3-4   length (always 0x0007)
 * 5     vendor
 * 6     function (0x93, 0xb0, 0xb1)
 * 7     target, the voice number for 0x93, always 1 for 0xb0 / 0xb1
 * 8     width, 0: the value is the one byte at 10, 1: it is the 16 bit at 10-11
 * 9     parameter number, 2 ~ 4
 * 10-11 value
 * </pre>
 * <p>
 * The width byte is what the corpus really settles: over the 231 messages of the
 * ~4400 files at {@code ~/Public/np2/mfi}, byte 11 is 0 in every one of the 88
 * messages whose byte 8 is 0, and byte 10 is 0 in every one of the 143 whose byte
 * 8 is 1 - never an exception either way. Reading it as a width makes the values
 * come out in useful ranges (1 ~ 63 for width 0, 0 ~ 7 for width 1) where a plain
 * 16 bit read leaves one of the two groups a multiple of 256. Which parameter
 * each number is, is not settled; {@link #getRawValue()} hands the two bytes out
 * untouched for whoever finds out.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
abstract class ParameterFunction extends FujitsuFunction {

    /** the value is the one byte at {@code data[10]} */
    public static final int WIDTH_8 = 0;
    /** the value is the 16 bit big endian at {@code data[10..11]} */
    public static final int WIDTH_16 = 1;

    /** for the log */
    abstract String getName();

    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.target    = data[7] & 0xff;
        this.width     = data[8] & 0xff;
        this.parameter = data[9] & 0xff;
        this.rawValue  = ((data[10] & 0xff) << 8) | (data[11] & 0xff);

logger.log(Level.DEBUG, "%s: target: %d, parameter: %d, value: %d (%d bit)"
        .formatted(getName(), target, parameter, getValue(), width == WIDTH_8 ? 8 : 16));
    }

    /** the voice number for 0x93, always 1 for 0xb0 / 0xb1 */
    private int target;
    /** @see #WIDTH_8 */
    private int width = WIDTH_16;
    /** 2 ~ 4 */
    private int parameter;
    /** the two value bytes as they are */
    private int rawValue;

    /** the voice number for 0x93, always 1 for 0xb0 / 0xb1 */
    public int getTarget() {
        return target;
    }

    /** @see #WIDTH_8 */
    public int getWidth() {
        return width;
    }

    /** 2 ~ 4 */
    public int getParameter() {
        return parameter;
    }

    /** the two value bytes as they are, big endian */
    public int getRawValue() {
        return rawValue;
    }

    /** the value {@link #getWidth()} says it is */
    public int getValue() {
        return width == WIDTH_8 ? (rawValue >> 8) & 0xff : rawValue;
    }

    /** */
    public void setTarget(int target) {
        this.target = target & 0xff;
    }

    /** @see #WIDTH_8 */
    public void setWidth(int width) {
        this.width = width;
    }

    /** */
    public void setParameter(int parameter) {
        this.parameter = parameter & 0xff;
    }

    /** the value {@link #getWidth()} says it is */
    public void setValue(int value) {
        this.rawValue = width == WIDTH_8 ? (value & 0xff) << 8 : value & 0xffff;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[7];
        tmp[0] = (byte) (getVendor() | CARRIER_DOCOMO);
        tmp[1] = (byte) getFunction();
        tmp[2] = (byte) target;
        tmp[3] = (byte) width;
        tmp[4] = (byte) parameter;
        tmp[5] = (byte) ((rawValue >> 8) & 0xff);
        tmp[6] = (byte) (rawValue & 0xff);
        return tmp;
    }
}
