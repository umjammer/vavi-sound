/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mfi5;

import java.lang.System.Logger.Level;
import java.util.Arrays;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.util.StringUtil;

import static vavi.sound.mfi.vavi.mfi5.Mfi5Sequencer.VENDOR_MFI5;


/**
 * MFi 5 System exclusive message function 0x40 processor.
 * (? a channel parameter)
 * <pre>
 * 0     delta
 * 1     ff
 * 2     ff
 * 3-4   length (always 0x000d)
 * 5     vendor
 * 6     0x40
 * 7     bit 7-6: kind (2 or 1), bit 1-0: channel in the track
 * 8     ? always 0
 * 9     ? 0 ~ 0x40, mostly 6, 12, 16, 18 or 24
 * 10-17 ?
 * </pre>
 * <p>
 * The message {@link vavi.sound.mfi.vavi.sharp.Function64} is, the same 11 bytes after the
 * function byte. Over the 120 of the corpus at {@code ~/Public/np2/mfi}:
 * </p>
 * <ul>
 *  <li>byte 7 is {@code 0x80 ~ 0x83}, {@code 0x41} or {@code 0x42}. Its low two bits are
 *      a channel number of the four a track has (the MFi note status way): the tracks
 *      other than 0 have them as well as track 0, so it is not a track wide one.</li>
 *  <li>byte 9 reads like a length in ticks, 24 in 41 of them, 12 and 18 in 14 each,
 *      the rest mostly multiples of 2 up to 64.</li>
 * </ul>
 * <p>
 * That is not enough to name anything (a vibrato, an LFO?). The payload is kept as it
 * is so that nothing MFi 5 falls through to
 * {@link vavi.sound.mfi.vavi.sequencer.UndefinedFunction}.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function64 implements MachineDependentFunction {

    @Override
    public String getId() {
        return VENDOR_MFI5 + "." + 0x40;
    }

    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        if (data.length < 8) {
            throw new InvalidMfiDataException("mfi5 0x40 is too short: " + data.length);
        }
        this.data = Arrays.copyOfRange(data, 7, data.length);
logger.log(Level.DEBUG, "mfi5 0x40: kind: %d, channel: %d\n%s".formatted(getKind(), getChannel(), StringUtil.getDump(this.data)));
    }

    /** the payload after the function byte */
    private byte[] data = new byte[0];

    /** the top two bits of the first byte, 2 or 1 */
    public int getKind() {
        return (data[0] & 0xc0) >> 6;
    }

    /** the channel in the track, 0 ~ 3 */
    public int getChannel() {
        return data[0] & 0x03;
    }

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
        tmp[0] = (byte) (VENDOR_MFI5 | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x40;
        System.arraycopy(data, 0, tmp, 2, data.length);
        return tmp;
    }
}
