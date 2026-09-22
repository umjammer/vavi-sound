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
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * Fujitsu System exclusive message function 0x92 processor.
 * (Voice Parameter of a wave table voice)
 * <p>
 * The same {@code offset, length, record} shape {@link Function145} has, with a 32
 * byte record: the {@code n}th voice of a file gets offset {@code 0x20 * n}. It
 * comes right after the {@link Function144} wave and the {@link Function145} wave
 * parameter of the same voice.
 * </p>
 * <p>
 * What the 32 bytes are, over the 48 messages of the ~4400 file corpus at
 * {@code ~/Public/np2/mfi} (the record is <b>not</b> a SMAF/MA VM35 voice, which is
 * 16 bytes - Fujitsu does not use a Yamaha sound source here, see
 * {@code RohmAudioEngine}):
 * </p>
 * <pre>
 *  + 0  always 0xff
 *  + 1  0xc0 | voice number, the same number the record offset gives (48 of 48)
 *  + 2  0x3c in 45 of 48, else 0x33 / 0x3f / 0x40 - a key number, 0x3c being
 *       middle C, so the key {@link Function145#getPitch()} belongs to
 *  + 3  ~ +12  0 but for a few files, +10 and +12 always 0
 *  +13 ~ +16  0x80 0x80 0x80 0x80 by default (+16 differs once)
 *  +17 ~ +20  0x7f 0x00 0x00 0x00 by default
 *  +21 ~ +24  0x80 0x80 0x80 0x80 by default (+21 never differs)
 *  +25 ~ +28  0x00 0x00 0x14 0x80 by default
 *  +29 ~ +31  0x7f 0x7f 0x00 by default
 * </pre>
 * <p>
 * The two {@code 0x80 0x80 0x80 0x80} runs each followed by four bytes look like
 * two envelopes (four rates, four levels), 0x80 being the neutral of a signed
 * byte, but nothing in the corpus settles which is which, so the record is handed
 * out as it is.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
public class Function146 extends FujitsuFunction {

    private static final Logger logger = getLogger(Function146.class.getName());

    /** the length of one record */
    public static final int RECORD_LENGTH = 0x20;

    @Override
    int getFunction() {
        return 0x92;
    }

    /**
     * 0x92 Voice Parameter    MFi4
     *
     * @param data see below
     * <pre>
     * 0      delta
     * 1      ff
     * 2      ff
     * 3-4    length
     * 5      vendor
     * 6      0x92
     * 7      offset of the record, 0x20 * voice number
     * 8      0x20, the length of the record
     * 9-40   the record, see the class comment
     * </pre>
     */
    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.offset = data[7] & 0xff;
        int length  = data[8] & 0xff;
        if (length != RECORD_LENGTH || data.length < 9 + length) {
logger.log(Level.WARNING, "unknown record length: %d, %d bytes follow".formatted(length, data.length - 9));
            length = Math.min(length, data.length - 9);
        }

        this.voiceParameter = new byte[length];
        System.arraycopy(data, 9, voiceParameter, 0, length);

logger.log(Level.DEBUG, "voice param[%d]: key: %d".formatted(getVoiceNumber(), getKeyNumber()));
logger.log(Level.TRACE, "voice param:\n" + StringUtil.getDump(voiceParameter));
    }

    /** byte offset of the record, {@link #RECORD_LENGTH} * voice number */
    private int offset;

    /** the 32 bytes */
    private byte[] voiceParameter = new byte[RECORD_LENGTH];

    /** the voice number this record belongs to */
    public int getVoiceNumber() {
        return offset / RECORD_LENGTH;
    }

    /** the key number the wave plays at its own pitch, 0x3c (middle C) in 45 of the 48 corpus messages */
    public int getKeyNumber() {
        return voiceParameter.length > 2 ? voiceParameter[2] & 0xff : -1;
    }

    /** the 32 bytes, see the class comment */
    public byte[] getVoiceParameter() {
        return voiceParameter;
    }

    /** the voice number this record belongs to */
    public void setVoiceNumber(int voiceNumber) {
        this.offset = (voiceNumber * RECORD_LENGTH) & 0xff;
    }

    /** @param voiceParameter the 32 bytes, see the class comment */
    public void setVoiceParameter(byte[] voiceParameter) {
        this.voiceParameter = voiceParameter;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[4 + voiceParameter.length];
        tmp[0] = (byte) (getVendor() | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x92;
        tmp[2] = (byte) offset;
        tmp[3] = (byte) voiceParameter.length;
        System.arraycopy(voiceParameter, 0, tmp, 4, voiceParameter.length);
        return tmp;
    }
}
