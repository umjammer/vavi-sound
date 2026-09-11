/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.lang.System.Logger.Level;
import java.util.Arrays;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.StringUtil;

import static vavi.sound.mfi.vavi.nec.NecSequencer.VENDOR_NEC;


/**
 * NEC System exclusive message function 0x02, 0xf0, 0x0c processor.
 * (MA-7 tone (voice) setting)
 * <p>
 * The message registers one voice for a (bank, program) slot, the slot the
 * {@link vavi.sound.mfi.vavi.track.ChangeBankMessage} and
 * {@link vavi.sound.mfi.vavi.track.ChangeVoiceMessage} of a channel select.
 * A voice may be split, in that case several messages share the same
 * (bank, program) and differ in {@link #getKeyHigh()}.
 * </p>
 * <p>
 * The voice parameters themselves are the MA-7 register image, the layout of
 * every {@link Type} is written down in the package readme. They cannot be
 * applied to the MIDI synthesizer the sequencer plays with, so this function
 * only decodes and keeps them.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see "tmp/SCP-MA-N-210-j/Bin/Plugin_SM7_N40/CnvMA7MFi_N.dll"
 */
public class Function2_240_12 implements MachineDependentFunction {

    /** bank, program, split value, key high */
    private static final int HEADER_LENGTH = 4;

    /** voice type, {@link #flags} plus the voice length tell them apart */
    public enum Type {
        /** FM 2 operator */
        FM_2Op(0x00, 24),
        /** FM 4 operator */
        FM_4Op(0x00, 44),
        /** wave table */
        WT(0x01, 18),
        /** FM 2 operator + AL (filter) */
        FM_2OpAL(0x02, 40),
        /** FM 4 operator + AL (filter) */
        FM_4OpAL(0x02, 60),
        /** wave table + AL (filter) */
        WTAL(0x03, 34);

        /** value of the first voice byte, bit 0: WT else FM, bit 1: AL section */
        public final int flags;
        /** including the first voice byte */
        public final int length;

        Type(int flags, int length) {
            this.flags = flags;
            this.length = length;
        }

        /** @return null when the combination is unknown */
        static Type valueOf(int flags, int length) {
            for (Type type : values()) {
                if (type.flags == flags && type.length == length) {
                    return type;
                }
            }
            return null;
        }
    }

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "2_240_12";
    }

    /**
     * 0x02, 0xf0, 0x0c ToneSetting
     *
     * @param message see below
     * <pre>
     * 0        delta
     * 1        ff
     * 2        ff
     * 3-4      length
     * 5        vendor
     *
     * 6        02
     * 7        f0
     * 8        ....1100
     *              ~~~~
     *              +------ 0xc
     *
     * 9        d.......    bank
     *          ~
     *          +---------- drum (rhythm) voice
     * 10       program, for a drum voice the drum index (note - 35)
     * 11       drum voice: the note number (program + 35),
     *          melody voice: 0, or the split region value
     * 12       upper key limit of the split region, 0 when not split
     * 13~      voice data, 13 is the type flags
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.bank     =  data[ 9] & 0x7f;
        this.drum     = (data[ 9] & 0x80) != 0;
        this.program  =  data[10] & 0xff;
        this.note     =  data[11] & 0xff;
        this.keyHigh  =  data[12] & 0xff;

        this.voice = Arrays.copyOfRange(data, 5 + 4 + HEADER_LENGTH, data.length);
        this.type = Type.valueOf(voice[0] & 0x03, voice.length);

logger.log(Level.DEBUG, "ToneSetting: bank: " + bank + (drum ? " (drum)" : "") + ", program: " + program +
        ", note: " + note + ", keyHigh: " + keyHigh + ", type: " + (type != null ? type : "unknown(%02x, %d)".formatted(voice[0], voice.length)));
logger.log(Level.TRACE, "voice:\n" + StringUtil.getDump(voice));
    }

    /** 0 ~ 127 */
    private int bank;
    /** drum (rhythm) voice */
    private boolean drum;
    /** 0 ~ 127, the drum index (note - 35) for a drum voice */
    private int program;
    /** the note number for a drum voice */
    private int note;
    /** upper key limit of the split region, 0 when the voice is not split */
    private int keyHigh;
    /** null when the type flags and the length do not match a known type */
    private Type type;
    /** MA-7 register image, {@code voice[0]} is the type flags */
    private byte[] voice;

    /** 0 ~ 127 */
    public int getBank() {
        return bank;
    }

    /** */
    public boolean isDrum() {
        return drum;
    }

    /** 0 ~ 127, the drum index (note - 35) for a drum voice */
    public int getProgram() {
        return program;
    }

    /** the note number for a drum voice */
    public int getNote() {
        return note;
    }

    /** upper key limit of the split region, 0 when the voice is not split */
    public int getKeyHigh() {
        return keyHigh;
    }

    /** @return null when the type flags and the length do not match a known type */
    public Type getType() {
        return type;
    }

    /** MA-7 register image, {@code voice[0]} is the type flags */
    public byte[] getVoice() {
        return voice;
    }

    /** */
    public void setBank(int bank) {
        this.bank = bank & 0x7f;
    }

    /** */
    public void setDrum(boolean drum) {
        this.drum = drum;
    }

    /** */
    public void setProgram(int program) {
        this.program = program & 0xff;
    }

    /** */
    public void setNote(int note) {
        this.note = note & 0xff;
    }

    /** */
    public void setKeyHigh(int keyHigh) {
        this.keyHigh = keyHigh & 0xff;
    }

    /** @param voice MA-7 register image, {@code voice[0]} is the type flags */
    public void setVoice(byte[] voice) {
        this.voice = voice;
        this.type = Type.valueOf(voice[0] & 0x03, voice.length);
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        if (voice == null) {
            throw new InvalidMfiDataException("voice is not set");
        }

        byte[] tmp = new byte[4 + HEADER_LENGTH + voice.length];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x02;
        tmp[2] = (byte) 0xf0;
        tmp[3] = (byte) 0x0c;
        tmp[4] = (byte) (bank | (drum ? 0x80 : 0x00));
        tmp[5] = (byte) program;
        tmp[6] = (byte) note;
        tmp[7] = (byte) keyHigh;

        System.arraycopy(voice, 0, tmp, 4 + HEADER_LENGTH, voice.length);

        return tmp;
    }
}
