/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sharp;

import java.lang.System.Logger.Level;
import java.util.Arrays;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;

import static vavi.sound.mfi.vavi.sharp.SharpSequencer.VENDOR_SHARP;


/**
 * Sharp System exclusive message function 0x12 processor.
 * (Wave Voice Setting)
 * <p>
 * The last of the three messages of a wave table voice, see {@link Function16}: which
 * bank and program the voice plays for.
 * </p>
 * <pre>
 * 0     delta
 * 1     ff
 * 2     ff
 * 3-4   length
 * 5     vendor
 * 6     0x12
 * 7     voice number
 * 8     parameter
 * 9     length of the value
 * 10-   value
 *
 * parameter 0x00, 4 bytes
 * 10    always 0x80
 * 11    drum, 0: melody, 1: drum
 * 12    bank
 * 13    program for a melody voice, the note key for a drum voice
 *
 * parameter 0x10, 1 byte
 * 10    ? always 0x80
 * </pre>
 * <p>
 * Over the 2241 of the corpus at {@code ~/Public/np2/mfi} (Sharp's and the same message
 * of Sony, 0x32) each voice number is one a {@link Function17} registers. Of the
 * parameter 0x00 ones, a melody voice's bank / program pair is one the song selects with
 * {@link vavi.sound.mfi.vavi.track.ChangeBankMessage} /
 * {@link vavi.sound.mfi.vavi.track.ChangeVoiceMessage} in 1220 of 1222 and a drum
 * voice's bank one it selects in 870 of 870, its last byte being a note key the song
 * plays on that bank in 870 of 870 - the same way the NEC and Sony tone messages address
 * a drum voice. Which switch parameter 0x10 is, is not settled (149 messages, 81 of the
 * 87 voices that have it are the ones whose {@link Function17} record byte 2 is 1).
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function18 implements MachineDependentFunction {

    /** bank and program */
    public static final int PARAMETER_PROGRAM = 0x00;
    /** ? */
    public static final int PARAMETER_10 = 0x10;

    /** the vendor id, the Sony subclass overrides it */
    protected int getVendor() {
        return VENDOR_SHARP;
    }

    /** the function byte, the Sony subclass overrides it */
    protected int getFunction() {
        return 0x12;
    }

    @Override
    public String getId() {
        return getVendor() + "." + getFunction();
    }

    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        if (data.length < 10) {
            throw new InvalidMfiDataException("wave voice setting is too short: " + data.length);
        }
        this.voiceNumber = data[7] & 0xff;
        this.parameter   = data[8] & 0xff;
        int length = Math.min(data[9] & 0xff, data.length - 10);
        this.value = Arrays.copyOfRange(data, 10, 10 + length);
        if (parameter == PARAMETER_PROGRAM && value.length >= 4) {
logger.log(Level.DEBUG, "wave voice setting: No.%d, bank: %d%s, program: %d".formatted(voiceNumber, getBank(), isDrum() ? " (drum)" : "", getProgram()));
        } else {
logger.log(Level.DEBUG, "wave voice setting: No.%d, parameter: 0x%02x, %d bytes".formatted(voiceNumber, parameter, value.length));
        }
    }

    /** */
    private int voiceNumber;
    /** @see #PARAMETER_PROGRAM */
    private int parameter;
    /** */
    private byte[] value = new byte[0];

    /** */
    public int getVoiceNumber() {
        return voiceNumber;
    }

    /** @see #PARAMETER_PROGRAM */
    public int getParameter() {
        return parameter;
    }

    /** the value as it is */
    public byte[] getValue() {
        return value;
    }

    /** {@link #PARAMETER_PROGRAM} only */
    public boolean isDrum() {
        return (value[1] & 0x01) != 0;
    }

    /** {@link #PARAMETER_PROGRAM} only */
    public int getBank() {
        return value[2] & 0xff;
    }

    /** the program of a melody voice, the note key of a drum voice, {@link #PARAMETER_PROGRAM} only */
    public int getProgram() {
        return value[3] & 0xff;
    }

    /** */
    public void setVoiceNumber(int voiceNumber) {
        this.voiceNumber = voiceNumber & 0xff;
    }

    /** makes this a {@link #PARAMETER_PROGRAM} */
    public void setProgram(int bank, boolean drum, int program) {
        this.parameter = PARAMETER_PROGRAM;
        this.value = new byte[] { (byte) 0x80, (byte) (drum ? 1 : 0), (byte) bank, (byte) program };
    }

    /** any other parameter */
    public void setValue(int parameter, byte[] value) {
        this.parameter = parameter & 0xff;
        this.value = value;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[5 + value.length];
        tmp[0] = (byte) (getVendor() | CARRIER_DOCOMO);
        tmp[1] = (byte) getFunction();
        tmp[2] = (byte) voiceNumber;
        tmp[3] = (byte) parameter;
        tmp[4] = (byte) value.length;
        System.arraycopy(value, 0, tmp, 5, value.length);
        return tmp;
    }
}
