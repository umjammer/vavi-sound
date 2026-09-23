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
import vavi.util.StringUtil;

import static vavi.sound.mfi.vavi.sharp.SharpSequencer.VENDOR_SHARP;


/**
 * Sharp System exclusive message function 0x11 processor.
 * (Wave Voice Parameter)
 * <p>
 * The second of the three messages of a wave table voice, see {@link Function16}.
 * </p>
 * <pre>
 * 0     delta
 * 1     ff
 * 2     ff
 * 3-4   length (always 0x0031)
 * 5     vendor
 * 6     0x11
 * 7     voice number
 * 8     ? always 2
 * 9     length of the record, always 0x2c
 * 10-   record
 *
 * record
 * + 0   1 in 1540 of 1553, else 2 or 3
 * + 1   the voice number again, 1553 of 1553
 * + 2   0 or 1
 * + 3   2 or 3 but for a few
 * + 4 ~ the voice parameters, not settled
 * </pre>
 * <p>
 * Over the 1553 of the corpus at {@code ~/Public/np2/mfi} (Sharp's and the same message
 * of Sony, 0x31), each voice number has the {@link Function16} wave of the same number.
 * Much of the rest of the record reads as 16 bit words whose low two bits are clear
 * ({@code 3f fc}, {@code 26 40}, {@code 20 00}), the way envelope rates and levels would,
 * but nothing in the corpus settles which is which, so {@link #getRecord()} hands the
 * bytes out as they are.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function17 implements MachineDependentFunction {

    /** the record length the corpus always has */
    public static final int RECORD_LENGTH = 0x2c;

    /** the vendor id, the Sony subclass overrides it */
    protected int getVendor() {
        return VENDOR_SHARP;
    }

    /** the function byte, the Sony subclass overrides it */
    protected int getFunction() {
        return 0x11;
    }

    @Override
    public String getId() {
        return getVendor() + "." + getFunction();
    }

    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        if (data.length < 10) {
            throw new InvalidMfiDataException("wave voice parameter is too short: " + data.length);
        }
        this.voiceNumber = data[7] & 0xff;
        this.type        = data[8] & 0xff;
        int length = Math.min(data[9] & 0xff, data.length - 10);
        this.record = Arrays.copyOfRange(data, 10, 10 + length);
logger.log(Level.DEBUG, "wave voice parameter: No.%d, %d bytes".formatted(voiceNumber, record.length));
logger.log(Level.TRACE, "record:\n" + StringUtil.getDump(record));
    }

    /** */
    private int voiceNumber;
    /** ? always 2 */
    private int type = 2;
    /** */
    private byte[] record = new byte[0];

    /** */
    public int getVoiceNumber() {
        return voiceNumber;
    }

    /** ? always 2 */
    public int getType() {
        return type;
    }

    /** the record as it is */
    public byte[] getRecord() {
        return record;
    }

    /** */
    public void setVoiceNumber(int voiceNumber) {
        this.voiceNumber = voiceNumber & 0xff;
    }

    /** */
    public void setType(int type) {
        this.type = type & 0xff;
    }

    /** */
    public void setRecord(byte[] record) {
        this.record = record;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[5 + record.length];
        tmp[0] = (byte) (getVendor() | CARRIER_DOCOMO);
        tmp[1] = (byte) getFunction();
        tmp[2] = (byte) voiceNumber;
        tmp[3] = (byte) type;
        tmp[4] = (byte) record.length;
        System.arraycopy(record, 0, tmp, 5, record.length);
        return tmp;
    }
}
