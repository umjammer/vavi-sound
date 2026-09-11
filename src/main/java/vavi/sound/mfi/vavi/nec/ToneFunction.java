/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;

import static vavi.sound.mfi.vavi.nec.NecSequencer.VENDOR_NEC;


/**
 * Base of the MFi 3.0 tone specification messages
 * (0x01, 0xf0, 0x04 / 0x05 / 0x08).
 * <p>
 * One message registers one or more voices, each as
 * </p>
 * <pre>
 *  [type] bank program &lt;voice ...&gt;
 * </pre>
 * <p>
 * {@code bank} matches the {@link vavi.sound.mfi.vavi.track.ChangeBankMessage}
 * data of the channel that plays the voice (bit 7 marks a drum voice) and
 * {@code program} the {@link vavi.sound.mfi.vavi.track.ChangeVoiceMessage} data,
 * the same way the MA-7 {@link Function2_240_12} header works. The type byte is
 * only there when the message can carry more than one voice shape.
 * </p>
 * <p>
 * The voice bytes are the SMAF/MA-5 voice image without its leading flags byte,
 * that is the <em>source</em> side of the conversion tables written down in the
 * package readme (the MA-7 messages carry the expanded <em>destination</em> side
 * instead).
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
abstract class ToneFunction implements MachineDependentFunction {

    /** one registered voice */
    public static class Tone {

        /** -1 when the message has no type byte */
        public final int type;
        /** 0 ~ 63 */
        public final int bank;
        /** drum (rhythm) voice */
        public final boolean drum;
        /** 0 ~ 127 */
        public final int program;
        /** the MA-5 voice image without its leading flags byte */
        public final byte[] voice;

        Tone(int type, int bank, int program, byte[] voice) {
            this.type = type;
            this.bank = bank & 0x3f;
            this.drum = (bank & 0x80) != 0;
            this.program = program;
            this.voice = voice;
        }

        /** */
        public Tone(int type, int bank, boolean drum, int program, byte[] voice) {
            this.type = type;
            this.bank = bank & 0x3f;
            this.drum = drum;
            this.program = program & 0xff;
            this.voice = voice;
        }

        int bankByte() {
            return bank | (drum ? 0x80 : 0x00);
        }

        @Override
        public String toString() {
            return (type >= 0 ? "type: " + type + ", " : "") +
                    "bank: " + bank + (drum ? " (drum)" : "") + ", program: " + program +
                    ", voice: " + voice.length + " bytes";
        }
    }

    /** the function nibble */
    abstract int getFunction();

    /** for the log */
    abstract String getName();

    /** whether a record starts with a type byte */
    abstract boolean hasType();

    /**
     * Length of the whole record (type, bank, program and voice) that starts at
     * {@code offset}, or -1 when it cannot be told.
     */
    abstract int getRecordLength(byte[] data, int offset, int remaining);

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "1_240_" + getFunction();
    }

    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        int header = hasType() ? 3 : 2;
        List<Tone> tones = new ArrayList<>();

        int p = 9;
        while (p < data.length) {
            int length = getRecordLength(data, p, data.length - p);
            if (length < header || p + length > data.length) {
                throw new InvalidMfiDataException(getName() + ": bad record at " + (p - 9) + " of " + (data.length - 9));
            }
            tones.add(new Tone(hasType() ? data[p] & 0xff : -1,
                    data[p + header - 2] & 0xff,
                    data[p + header - 1] & 0xff,
                    Arrays.copyOfRange(data, p + header, p + length)));
            p += length;
        }

        this.tones = tones;

logger.log(Level.DEBUG, getName() + ": " + tones.size() + " voice(s): " +
        tones.stream().map(Tone::toString).collect(Collectors.joining("; ")));
    }

    /** */
    private List<Tone> tones = new ArrayList<>();

    /** */
    public List<Tone> getTones() {
        return tones;
    }

    /** */
    public void setTones(List<Tone> tones) {
        this.tones = tones;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        int header = hasType() ? 3 : 2;
        int length = 4;
        for (Tone tone : tones) {
            length += header + tone.voice.length;
        }

        byte[] tmp = new byte[length];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) 0xf0;
        tmp[3] = (byte) getFunction();

        int p = 4;
        for (Tone tone : tones) {
            if (hasType()) {
                tmp[p++] = (byte) tone.type;
            }
            tmp[p++] = (byte) tone.bankByte();
            tmp[p++] = (byte) tone.program;
            System.arraycopy(tone.voice, 0, tmp, p, tone.voice.length);
            p += tone.voice.length;
        }

        return tmp;
    }
}
