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
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * Fujitsu System exclusive message function 0x02 processor.
 * (unidentified)
 * <p>
 * This one is here so that nothing Fujitsu falls through to
 * {@link vavi.sound.mfi.vavi.sequencer.UndefinedFunction} any more, <b>not</b>
 * because the corpus settles anything: the only file of the ~4400 at
 * {@code ~/Public/np2/mfi} that writes it has no {@code exst} sub chunk, so
 * {@link vavi.sound.mfi.vavi.VaviMfiFileFormat} cannot read it at all, and scanning
 * its track by hand turns up just two messages, {@code 21 02 62 00} and
 * {@code 21 02 02 00}, next to the {@link Function1} voices of the same file.
 * </p>
 * <p>
 * Two bytes each, the second one 0 in both. What they are is anybody's guess, so the
 * payload is handed out as it is.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
public class Function2 extends FujitsuFunction {

    private static final Logger logger = getLogger(Function2.class.getName());

    @Override
    int getFunction() {
        return 0x02;
    }

    /**
     * 0x02    MFi2
     *
     * @param data see below
     * <pre>
     * 0      delta
     * 1      ff
     * 2      ff
     * 3-4    length
     * 5      vendor
     * 6      0x02
     * 7-     as they are, two bytes in the corpus
     * </pre>
     */
    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.data = Arrays.copyOfRange(data, 7, data.length);
logger.log(Level.DEBUG, "unidentified: %d bytes".formatted(this.data.length));
logger.log(Level.TRACE, "data:\n" + StringUtil.getDump(this.data, 64));
    }

    /** everything after the function byte */
    private byte[] data = new byte[0];

    /** everything after the function byte */
    public byte[] getData() {
        return data;
    }

    /** @param data everything after the function byte */
    public void setData(byte[] data) {
        this.data = data;
    }

    /** @before {@link #setData(byte[])} */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[2 + data.length];
        tmp[0] = (byte) (VENDOR_FUJITSU | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x02;
        System.arraycopy(data, 0, tmp, 2, data.length);
        return tmp;
    }
}
