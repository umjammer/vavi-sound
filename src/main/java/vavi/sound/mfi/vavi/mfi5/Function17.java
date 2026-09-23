/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mfi5;

import static vavi.sound.mfi.vavi.mfi5.Mfi5Sequencer.VENDOR_MFI5;


/**
 * MFi 5 System exclusive message function 0x11 processor.
 * (Wave Voice Parameter)
 * <p>
 * The message Sharp writes as 0x11, {@link vavi.sound.mfi.vavi.sharp.Function17} reads
 * it. What Sharp only writes one way, the MFi 5 writer writes with other parameter
 * numbers in the byte Sharp's class calls the type, so the byte after it is the length
 * of the value in all of them:
 * </p>
 * <pre>
 * 0     delta
 * 1     ff
 * 2     ff
 * 3-4   length
 * 5     vendor
 * 6     0x11
 * 7     voice number
 * 8     parameter ({@link #getType()})
 * 9     length of the value
 * 10-   value ({@link #getRecord()})
 *
 * parameter 0x02, 44 bytes, the whole voice, as Sharp's
 * + 0   flags, bit 0: an uploaded wave, bit 1: the second voice of a pair
 * + 1   the voice number again
 * + 2-5 the preset the voice is derived from, + 3 bank, + 4 program
 * + 8   link, the voice of oscillator B
 * + 9   oscillator balance
 * + 12  env A, 7 bytes
 * + 27  shape w4, 2 bytes
 *
 * the others are those bytes of the record alone
 * parameter 0x10, 6 bytes, + 0 ~ + 5: a voice of a built in tone (bit 0 clear), no wave
 * parameter 0x20, 1 byte,  + 8: link, the second voice of the pair
 * parameter 0x21, 1 byte,  + 9: oscillator balance (0x7e, 0x50 or 0x28)
 * parameter 0x40, 7 bytes, + 12 ~ + 18: env A
 * parameter 0x52, 2 bytes, + 27 ~ + 28: shape w4
 * </pre>
 * <p>
 * The record layout is the one of the fuetrek sound source's voice edit parameters,
 * see {@code vavi.sound.fuetrek.FuetrekVoice.Template} of {@code vavi-apps-mfiplayer},
 * which plays these voices ({@code vavi.sound.mfi.fuetrek.UcsSequencer}).
 * </p>
 * <p>
 * Read from the 191 MFi 5 files of the corpus at {@code ~/Public/np2/mfi} (no document
 * being available):
 * </p>
 * <ul>
 *  <li>a pair: parameter 0x20 of voice {@code n} is {@code n + 1}, 11 of 11, and the voice
 *      it names has bit 1 of the flags set. Of the 53 records and parameters 0x10 whose
 *      bit 1 is set none has a {@link Function18} of its own, of the 415 whose bit 1 is
 *      clear all but 18 have. So the second voice sounds with the first one, for the
 *      bank and program of the first one, two voices layered.</li>
 *  <li>parameter 0x10 comes for voices that have no {@link Function16} wave, its bank /
 *      program is the one the {@link Function18} of the pair says, 22 of 22.</li>
 *  <li>parameter 0x10 is the record + 0 ~ + 5 of a voice with no wave: its + 0 is 0 or 2,
 *      where a record's is 1 or 3 (bit 0, an uploaded wave).</li>
 *  <li>a record's + 9 not 0 comes with + 8 = the next voice, a second one, 30 of 30;
 *      + 8 of a record whose + 9 is 0 says nothing.</li>
 *  <li>parameter 0x40 comes after the voices are set up, a voice changed while the song
 *      plays. Of the two voices that have a record too, one has the record +12 ~ +18
 *      as it is, the other has it with its first word, then its third changed.</li>
 *  <li>parameter 0x52 values (0x2550 ~ 0x2e10) are in the range of record +27
 *      (0x2000 ~ 0x3000), no voice has both.</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function17 extends vavi.sound.mfi.vavi.sharp.Function17 {

    /** the whole voice, 44 bytes */
    public static final int PARAMETER_RECORD = 0x02;
    /** a voice of a built in tone, 6 bytes */
    public static final int PARAMETER_PRESET = 0x10;
    /** the second voice of a pair, 1 byte */
    public static final int PARAMETER_PAIR = 0x20;
    /** oscillator balance, record + 9, 1 byte */
    public static final int PARAMETER_BALANCE = 0x21;
    /** env A, record +12 ~ +18, 7 bytes */
    public static final int PARAMETER_ENVELOPE = 0x40;
    /** shape w4, record + 27 ~ + 28, 2 bytes */
    public static final int PARAMETER_SHAPE_W4 = 0x52;

    /** the flags bit of the second voice of a pair */
    private static final int FLAG_SECOND = 0x02;

    @Override
    protected int getVendor() {
        return VENDOR_MFI5;
    }

    /** the parameter number, {@link #getType()} named the MFi 5 way */
    public int getParameter() {
        return getType();
    }

    /** the second voice of a pair, {@link #PARAMETER_RECORD} and {@link #PARAMETER_PRESET} only */
    public boolean isSecond() {
        return (getRecord()[0] & FLAG_SECOND) != 0;
    }

    /** {@link #PARAMETER_PRESET} only */
    public int getPresetBank() {
        return getRecord()[3] & 0xff;
    }

    /** {@link #PARAMETER_PRESET} only */
    public int getPresetProgram() {
        return getRecord()[4] & 0xff;
    }

    /**
     * @return where in the 44 byte record a parameter is, -1: not known
     */
    public static int offsetOf(int parameter) {
        return switch (parameter) {
            case PARAMETER_RECORD, PARAMETER_PRESET -> 0;
            case PARAMETER_PAIR -> 8;
            case PARAMETER_BALANCE -> 9;
            case PARAMETER_ENVELOPE -> 12;
            case PARAMETER_SHAPE_W4 -> 27;
            default -> -1;
        };
    }

    /** the second voice, {@link #PARAMETER_PAIR} only */
    public int getPairVoiceNumber() {
        return getRecord()[0] & 0xff;
    }
}
