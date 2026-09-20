/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.fujitsu;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;

import static java.lang.System.getLogger;


/**
 * Fujitsu System exclusive message function 0x91 processor.
 * (Wave Parameter, how the {@link Function144} wave is played)
 * <p>
 * The message writes one 10 byte record into a table, {@code data[7]} is the byte
 * offset of the record and {@code data[8]} its length - the {@code n}th wave of a
 * file gets offset {@code 10 * n}. {@link Function146} is the same shape with a 32
 * byte record.
 * </p>
 * <p>
 * How the record was read, since no MFi document was available (all counts are
 * over the 48 messages of the ~4400 file corpus at {@code ~/Public/np2/mfi}):
 * </p>
 * <ul>
 *  <li>the first field is {@code 0x8000 | (waveOffset &gt;&gt; 2)} for all 48,
 *      {@code waveOffset} being the destination offset of the {@link Function144}
 *      that came right before - so it is the start address of the wave in 4 byte
 *      words, the top bit presumably telling wave memory from a rom wave.</li>
 *  <li>the next two fields satisfy {@code 0 <= loopStart <= loopEnd < waveLength}
 *      for all 48, and {@code loopEnd} is within a few bytes of the end of the
 *      wave in most of them - a one shot attack with a short sustain loop.</li>
 *  <li>the last two fields always appear as a pair (17 distinct pairs, no high
 *      word ever shows up with two different low words and vice versa), so they
 *      are one 32 bit number, low word first. Read as {@code value / 0x01000000}
 *      it is 1.0 exactly when the wave is a flat single cycle one and 0.61 ~ 3.39
 *      otherwise, and 8 of the 17 values are an exact equal tempered semitone
 *      ratio (-4, -3, 0, +1, +2, +5, +7, +9) - a playback pitch factor.</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
public class Function145 extends FujitsuFunction {

    private static final Logger logger = getLogger(Function145.class.getName());

    /** the length of one record */
    public static final int RECORD_LENGTH = 0x0a;

    /** {@link #getStartAddress()} is in 4 byte words */
    public static final int ADDRESS_UNIT = 4;

    /** set in {@link #getRawStartAddress()} by every message of the corpus */
    public static final int ADDRESS_FLAG = 0x8000;

    /** {@link #getPitch()} of 1.0 */
    public static final int PITCH_ONE = 0x0100_0000;

    @Override
    int getFunction() {
        return 0x91;
    }

    /**
     * 0x91 Wave Parameter    MFi4
     *
     * @param data see below
     * <pre>
     * 0      delta
     * 1      ff
     * 2      ff
     * 3-4    length
     * 5      vendor
     * 6      0x91
     * 7      offset of the record, 0x0a * wave number
     * 8      0x0a, the length of the record
     * 9-10   0x8000 | start address of the wave in 4 byte words
     * 11-12  loop start [byte, from the start of the wave]
     * 13-14  loop end   [byte, from the start of the wave]
     * 15-16  pitch, low 16 bits
     * 17-18  pitch, high 16 bits, 0x0100_0000 is 1.0
     * </pre>
     */
    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.offset = data[7] & 0xff;
        int length  = data[8] & 0xff;
        if (length != RECORD_LENGTH) {
logger.log(Level.WARNING, "unknown record length: " + length);
        }

        this.rawStartAddress = ((data[9]  & 0xff) << 8) | (data[10] & 0xff);
        this.loopStart       = ((data[11] & 0xff) << 8) | (data[12] & 0xff);
        this.loopEnd         = ((data[13] & 0xff) << 8) | (data[14] & 0xff);
        int pitchLow         = ((data[15] & 0xff) << 8) | (data[16] & 0xff);
        int pitchHigh        = ((data[17] & 0xff) << 8) | (data[18] & 0xff);
        this.rawPitch = (pitchHigh << 16) | pitchLow;

logger.log(Level.DEBUG, "wave param[%d]: @%04x, loop: %04x ~ %04x, pitch: %.5f"
        .formatted(offset / RECORD_LENGTH, getStartAddress(), loopStart, loopEnd, getPitch()));
    }

    /** byte offset of the record, {@link #RECORD_LENGTH} * wave number */
    private int offset;
    /** {@link #ADDRESS_FLAG} | start address in {@link #ADDRESS_UNIT} byte words */
    private int rawStartAddress;
    /** [byte, from the start of the wave] */
    private int loopStart;
    /** [byte, from the start of the wave] */
    private int loopEnd;
    /** {@link #PITCH_ONE} is 1.0 */
    private int rawPitch = PITCH_ONE;

    /** the wave number this record belongs to */
    public int getWaveNumber() {
        return offset / RECORD_LENGTH;
    }

    /** {@link #ADDRESS_FLAG} | start address in {@link #ADDRESS_UNIT} byte words */
    public int getRawStartAddress() {
        return rawStartAddress;
    }

    /** the {@link Function144#getOffset()} this record plays */
    public int getStartAddress() {
        return (rawStartAddress & (ADDRESS_FLAG - 1)) * ADDRESS_UNIT;
    }

    /** [byte, from the start of the wave] */
    public int getLoopStart() {
        return loopStart;
    }

    /** [byte, from the start of the wave] */
    public int getLoopEnd() {
        return loopEnd;
    }

    /** {@link #PITCH_ONE} is 1.0 */
    public int getRawPitch() {
        return rawPitch;
    }

    /** playback pitch factor, 1.0 is the wave as it is */
    public double getPitch() {
        return (rawPitch & 0xffff_ffffL) / (double) PITCH_ONE;
    }

    /** the wave number this record belongs to */
    public void setWaveNumber(int waveNumber) {
        this.offset = (waveNumber * RECORD_LENGTH) & 0xff;
    }

    /** @param startAddress the {@link Function144#getOffset()} to play */
    public void setStartAddress(int startAddress) {
        this.rawStartAddress = ADDRESS_FLAG | ((startAddress / ADDRESS_UNIT) & (ADDRESS_FLAG - 1));
    }

    /** @param loopStart [byte, from the start of the wave] */
    public void setLoopStart(int loopStart) {
        this.loopStart = loopStart & 0xffff;
    }

    /** @param loopEnd [byte, from the start of the wave] */
    public void setLoopEnd(int loopEnd) {
        this.loopEnd = loopEnd & 0xffff;
    }

    /** @param pitch playback pitch factor, 1.0 is the wave as it is */
    public void setPitch(double pitch) {
        this.rawPitch = (int) Math.round(pitch * PITCH_ONE);
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[4 + RECORD_LENGTH];
        tmp[0]  = (byte) (VENDOR_FUJITSU | CARRIER_DOCOMO);
        tmp[1]  = (byte) 0x91;
        tmp[2]  = (byte) offset;
        tmp[3]  = (byte) RECORD_LENGTH;
        tmp[4]  = (byte) ((rawStartAddress >> 8) & 0xff);
        tmp[5]  = (byte) (rawStartAddress & 0xff);
        tmp[6]  = (byte) ((loopStart >> 8) & 0xff);
        tmp[7]  = (byte) (loopStart & 0xff);
        tmp[8]  = (byte) ((loopEnd >> 8) & 0xff);
        tmp[9]  = (byte) (loopEnd & 0xff);
        tmp[10] = (byte) ((rawPitch >> 8) & 0xff);
        tmp[11] = (byte) (rawPitch & 0xff);
        tmp[12] = (byte) ((rawPitch >> 24) & 0xff);
        tmp[13] = (byte) ((rawPitch >> 16) & 0xff);
        return tmp;
    }
}
