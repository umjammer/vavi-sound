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
 * Sharp System exclusive message function 0x40 processor.
 * (? unidentified)
 * <pre>
 * 0     delta
 * 1     ff
 * 2     ff
 * 3-4   length (always 0x000d)
 * 5     vendor
 * 6     0x40
 * 7-17  ?
 * </pre>
 * <p>
 * The corpus at {@code ~/Public/np2/mfi} has 41 of them in 23 files, always 11 bytes
 * after the function byte, e.g. {@code 80 00 0d 00 00 00 20 0c 00 08 04} (11 times) or
 * {@code c1 00 20 00 00 00 a3 00 00 00 00}. They come right after the
 * {@link vavi.sound.mfi.vavi.track.ChangeBankMessage} /
 * {@link vavi.sound.mfi.vavi.track.ChangeVoiceMessage} pair of a voice, so they belong
 * to a voice, and the top two bits of the first byte vary like a voice field would,
 * but that is not enough to name anything. The payload is kept as it is so that
 * nothing Sharp falls through to
 * {@link vavi.sound.mfi.vavi.sequencer.UndefinedFunction}.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function64 implements MachineDependentFunction {

    @Override
    public String getId() {
        return VENDOR_SHARP + "." + 0x40;
    }

    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.data = Arrays.copyOfRange(data, 7, data.length);
logger.log(Level.DEBUG, "sharp 0x40: " + StringUtil.getDump(this.data));
    }

    /** the payload after the function byte */
    private byte[] data = new byte[0];

    /** the payload after the function byte as it is */
    public byte[] getData() {
        return data;
    }

    /** */
    public void setData(byte[] data) {
        this.data = data;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[2 + data.length];
        tmp[0] = (byte) (VENDOR_SHARP | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x40;
        System.arraycopy(data, 0, tmp, 2, data.length);
        return tmp;
    }
}
