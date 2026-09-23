/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.fujitsu;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.ChangeBankMessage;
import vavi.sound.mfi.vavi.track.ChangeVoiceMessage;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * Fujitsu System exclusive message function 0x01 processor.
 * (the MFi 2.0 sound source setup, a group of its own)
 * <p>
 * Unlike every other Fujitsu function this one has a sub function byte after the
 * function byte and is really a small vocabulary:
 * </p>
 * <pre>
 *  ff ff &lt;len:2&gt; 21 01 &lt;sub&gt; &lt;payload...&gt;
 * </pre>
 * <table>
 *  <caption>the sub functions of the corpus</caption>
 *  <tr><th>sub</th><th>payload</th><th>count</th><th>meaning</th></tr>
 *  <tr><td>0x01</td><td>16</td><td>8</td><td>{@link #SUB_VOICE_1 voice}, the index field holds the program</td></tr>
 *  <tr><td>0x02</td><td>16</td><td>59</td><td>{@link #SUB_VOICE_2 voice}, the index field counts up</td></tr>
 *  <tr><td>0x03</td><td>9751</td><td>1</td><td>{@link #SUB_WAVE a wave}, 4 bit adpcm by the look of it</td></tr>
 *  <tr><td>0x05</td><td>2</td><td>4</td><td>{@link #SUB_CONTROL ?}, {@code 01 ff} three times then {@code 01 28}</td></tr>
 *  <tr><td>0x06</td><td>1</td><td>1</td><td>{@link #SUB_VOLUME ?}, {@code 3f}, the top of a 6 bit range</td></tr>
 * </table>
 * <p>
 * Only the voice form is settled, and it is settled well - the numbers are over the
 * 67 voices of the ~4400 file corpus at {@code ~/Public/np2/mfi}:
 * </p>
 * <ul>
 *  <li>{@link #getBank()} is a bank the file really selects with
 *      {@link ChangeBankMessage} (67 of 67), and its bit 7 is the drum flag the way
 *      the NEC tone messages set it.</li>
 *  <li>{@link #getProgram()} of a melody voice is a program the file really selects
 *      with {@link ChangeVoiceMessage} (43 of the 45 melody voices, the two left over
 *      being one file that registers a voice it never plays). A drum voice's is not,
 *      it is the index inside the kit - again as in the NEC messages, where the drum
 *      program is the kit index and the key number is carried beside it.</li>
 *  <li>{@link #getKeyNumber()} is non zero exactly when the drum flag is set (67 of
 *      67), i.e. the fixed key a drum voice plays at. The corpus grown to 51834 voices
 *      of 4879 files still says so but for 13 drum voices with key 0, and those 13 are
 *      all {@link #isEmpty() empty} slots (drum programs 0x32 ~ 0x34) of files the
 *      {@code Yamaha S2M_0100} converter wrote - a placeholder, not a voice.</li>
 *  <li>{@link #getIndex()} counts 0, 1, 2 ... over the messages of a file for sub
 *      0x02 (59 of 59). For sub 0x01 it is the program instead (8 of 8).</li>
 * </ul>
 * <p>
 * The 12 voice bytes are one byte whose low 3 bits are 0 in 66 of the 67 (a panpot in
 * bits 7-3 would look like that, as the MA-5 voice's {@code Panpot, BO} byte does),
 * then 0x01 in all 67, then two 5 byte operators - a 2 operator FM voice. Which field
 * of an operator is which is not settled, so {@link #getVoice()} hands them out as
 * they are.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
public class Function1 extends FujitsuFunction {

    private static final Logger logger = getLogger(Function1.class.getName());

    /** a voice whose {@link #getIndex()} is the program */
    public static final int SUB_VOICE_1 = 0x01;
    /** a voice whose {@link #getIndex()} counts up */
    public static final int SUB_VOICE_2 = 0x02;
    /** a wave, 4 bit adpcm by the look of it */
    public static final int SUB_WAVE = 0x03;
    /** two bytes, a playback control by where it sits */
    public static final int SUB_CONTROL = 0x05;
    /** one byte, 0x3f - the top of a 6 bit range, so a volume by the look of it */
    public static final int SUB_VOLUME = 0x06;

    /** the payload length of {@link #SUB_VOICE_1} / {@link #SUB_VOICE_2} */
    public static final int VOICE_MESSAGE_LENGTH = 16;

    /** {@link #getVoice()}, that is {@link #VOICE_MESSAGE_LENGTH} less the 4 header bytes */
    public static final int VOICE_LENGTH = 12;

    /** the voice carries two of these */
    public static final int OPERATOR_LENGTH = 5;

    @Override
    int getFunction() {
        return 0x01;
    }

    /**
     * 0x01    MFi2
     *
     * @param data see below
     * <pre>
     * 0      delta
     * 1      ff
     * 2      ff
     * 3-4    length
     * 5      vendor
     * 6      0x01
     * 7      sub function
     *
     * sub 0x01 / 0x02
     * 8      key number, the key a drum voice plays at, 0 for a melody voice
     * 9      index, counts up over a file (0x02) or the program (0x01)
     * 10     DBBBBBBB
     *          D: drum
     *          BBBBBBB: bank, matches ChangeBankMessage
     * 11     program, matches ChangeVoiceMessage for a melody voice,
     *        the index inside the kit for a drum voice
     * 12-23  the voice, two 5 byte fm operators behind two bytes
     *
     * other subs
     * 8-     as they are
     * </pre>
     */
    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.subFunction = data[7] & 0xff;
        this.data = Arrays.copyOfRange(data, 8, data.length);

        if (isVoice()) {
            if (this.data.length != VOICE_MESSAGE_LENGTH) {
logger.log(Level.WARNING, "voice payload: %d, expected %d".formatted(this.data.length, VOICE_MESSAGE_LENGTH));
                return;
            }
logger.log(Level.DEBUG, "voice[%d]: bank: %d%s, program: %d, key: %d"
        .formatted(getIndex(), getBank(), isDrum() ? " (drum)" : "", getProgram(), getKeyNumber()));
logger.log(Level.TRACE, "voice:\n" + StringUtil.getDump(getVoice()));
        } else {
logger.log(Level.DEBUG, "sub function 0x%02x: %d bytes".formatted(subFunction, this.data.length));
logger.log(Level.TRACE, "data:\n" + StringUtil.getDump(this.data, 64));
        }
    }

    /** @see #SUB_VOICE_1 */
    private int subFunction;

    /** everything after the sub function byte */
    private byte[] data = new byte[0];

    /** @see #SUB_VOICE_1 */
    public int getSubFunction() {
        return subFunction;
    }

    /** is this one of the two voice forms? */
    public boolean isVoice() {
        return subFunction == SUB_VOICE_1 || subFunction == SUB_VOICE_2;
    }

    /** everything after the sub function byte */
    public byte[] getData() {
        return data;
    }

    /** the key a drum voice plays at, 0 for a melody voice */
    public int getKeyNumber() {
        return data[0] & 0xff;
    }

    /** counts up over the messages of a file for {@link #SUB_VOICE_2}, the program for {@link #SUB_VOICE_1} */
    public int getIndex() {
        return data[1] & 0xff;
    }

    /** matches the {@link ChangeBankMessage} data of the channel that uses the voice */
    public int getBank() {
        return data[2] & 0x7f;
    }

    /** a drum (rhythm) voice */
    public boolean isDrum() {
        return (data[2] & 0x80) != 0;
    }

    /** the {@link ChangeVoiceMessage} data for a melody voice, the kit index for a drum one */
    public int getProgram() {
        return data[3] & 0xff;
    }

    /** the {@link #VOICE_LENGTH} voice bytes, two {@link #OPERATOR_LENGTH} byte operators behind two bytes */
    public byte[] getVoice() {
        return Arrays.copyOfRange(data, 4, data.length);
    }

    /** @param operator 0 or 1 */
    public byte[] getOperator(int operator) {
        int offset = 4 + (VOICE_LENGTH - 2 * OPERATOR_LENGTH) + operator * OPERATOR_LENGTH;
        return Arrays.copyOfRange(data, offset, offset + OPERATOR_LENGTH);
    }

    /**
     * Whether the voice is an empty slot: both operators all 0 but their last byte, e.g.
     * {@code 01 00 00 00 00 a0 00 00 00 00 a0}. The {@code Yamaha S2M_0100} converter
     * registers such slots, drum ones with key number 0.
     */
    public boolean isEmpty() {
        for (int operator = 0; operator < 2; operator++) {
            byte[] bytes = getOperator(operator);
            for (int i = 0; i < OPERATOR_LENGTH - 1; i++) {
                if (bytes[i] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /** @see #SUB_VOICE_1 */
    public void setSubFunction(int subFunction) {
        this.subFunction = subFunction & 0xff;
    }

    /** @param data everything after the sub function byte */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * @before {@link #setSubFunction(int)}
     * @before {@link #setData(byte[])}
     */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[3 + data.length];
        tmp[0] = (byte) (getVendor() | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) subFunction;
        System.arraycopy(data, 0, tmp, 3, data.length);
        return tmp;
    }
}
