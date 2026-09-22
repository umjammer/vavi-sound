/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sony;

import java.lang.System.Logger.Level;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;


/**
 * Base of the Sony pitch bend messages (0xe0 ~ 0xe3, 0xe8 ~ 0xeb).
 * <p>
 * The Sony MFi 2.0 writer ({@code _so16} files) puts a 14 bit pitch bend in two
 * messages, the MIDI way: the low 2 bits of the function byte are the voice, bit 3
 * tells the halves apart.
 * </p>
 * <pre>
 * 0     delta
 * 1     ff
 * 2     ff
 * 3-4   length (0x0003, or 0x0004 for the two byte form)
 * 5     vendor
 * 6     function, 0xe0 + voice: LSB, 0xe8 + voice: MSB
 * 7     .6543210  LSB for 0xe0 + voice, MSB for 0xe8 + voice
 *                 (the two byte form of 0xe8 + voice: LSB)
 * 8     .6543210  the two byte form of 0xe8 + voice only: MSB
 * </pre>
 * <p>
 * The corpus at {@code ~/Public/np2/mfi} settles it:
 * </p>
 * <ul>
 *  <li>every 0xe0 + voice (107430) is followed at the same delta by the 0xe8 + voice of
 *      the same voice (107430 one byte ones), so the LSB is cached and the MSB commits,
 *      as in MIDI. The two byte 0xe8 + voice (7725) comes alone and carries both.</li>
 *  <li>the {@code upload_melody} directories carry every song once per maker. Over
 *      the 244 of these messages in {@code 0_A645_C194_01_NULL_01_so16.mld}, the 122
 *      values {@code (msb << 7) | lsb} are, point by point, the 0xe9 / 0xe4 pitch bend of
 *      the NEC {@code _n40} file of the same song ({@code 0x2000} at rest) within the 3
 *      bits the NEC one loses, e.g. {@code 1 4097 6001 8192} against
 *      {@code 0 4096 6000 8192}. The MSB byte rests at 0x40.</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
abstract class PitchBendFunction extends SonyFunction {

    /** the function byte of the LSB message of voice 0 */
    public static final int LSB = 0xe0;
    /** the function byte of the MSB message of voice 0 */
    public static final int MSB = 0xe8;
    /** the 14 bit value at rest */
    public static final int CENTER = 0x2000;

    /** @return true for 0xe8 ~ 0xeb */
    boolean isMsb() {
        return (getFunction() & 0x08) != 0;
    }

    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        if (isMsb()) {
            if (data.length > 8) {
                this.lsb = data[7] & 0x7f;
                this.msb = data[8] & 0x7f;
            } else {
                this.lsb = -1;
                this.msb = data[7] & 0x7f;
            }
        } else {
            this.lsb = data[7] & 0x7f;
            this.msb = -1;
        }
logger.log(Level.DEBUG, "Pitch Bend: %dch, lsb: %d, msb: %d".formatted(getVoice(), lsb, msb));
    }

    /** 0 ~ 127, -1 when this message does not carry it */
    private int lsb = -1;
    /** 0 ~ 127, -1 when this message does not carry it */
    private int msb = -1;

    /** 0 ~ 3 */
    public int getVoice() {
        return getFunction() & 0x03;
    }

    /** 0 ~ 127, -1 when this message does not carry it */
    public int getLsb() {
        return lsb;
    }

    /** 0 ~ 127, -1 when this message does not carry it */
    public int getMsb() {
        return msb;
    }

    /**
     * @param lsb the LSB this voice got the last from a 0xe0 + voice, used when this
     *            message carries none
     * @return the 14 bit value, {@link #CENTER} at rest
     * @throws IllegalStateException this is an LSB message, which commits nothing
     */
    public int getValue(int lsb) {
        if (msb < 0) {
            throw new IllegalStateException("an LSB message commits no value");
        }
        return (msb << 7) | (this.lsb < 0 ? lsb & 0x7f : this.lsb);
    }

    /** for the LSB message and the one byte MSB message */
    public void setValue(int value) {
        if (isMsb()) {
            this.msb = value & 0x7f;
            this.lsb = -1;
        } else {
            this.lsb = value & 0x7f;
        }
    }

    /** makes an MSB message the two byte form, which carries all 14 bits */
    public void setValue14(int value) {
        if (!isMsb()) {
            throw new IllegalStateException("only an MSB message carries 14 bit");
        }
        this.msb = (value >> 7) & 0x7f;
        this.lsb = value & 0x7f;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        boolean both = isMsb() && lsb >= 0;
        byte[] tmp = new byte[both ? 4 : 3];
        tmp[0] = (byte) (VENDOR_SONY | CARRIER_DOCOMO);
        tmp[1] = (byte) getFunction();
        if (both) {
            tmp[2] = (byte) lsb;
            tmp[3] = (byte) msb;
        } else {
            tmp[2] = (byte) (isMsb() ? msb : lsb);
        }
        return tmp;
    }
}
