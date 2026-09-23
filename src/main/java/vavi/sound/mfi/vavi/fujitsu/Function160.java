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
 * Fujitsu System exclusive message function 0xa0 processor.
 * (a sound source wide setting)
 * <p>
 * One data byte. 12 messages in 12 files of the ~4400 file corpus at
 * {@code ~/Public/np2/mfi}, never twice in a file, always once at the head of
 * track 0 - after the wave table voice block ({@link Function144} ~
 * {@link Function147}) when there is one and before the first
 * {@link vavi.sound.mfi.vavi.track.ChangeBankMessage} of the song. The value is 1
 * (7 files), 2, 3 (2 files), 4 or 5, so it is a small mode or count and not a
 * level, but which is not settled.
 * </p>
 * <p>
 * Unlike {@link Function161} the value has no channel in its top two bits: 1 ~ 5
 * would all be channel 0 there, and the message comes before any channel is set
 * up.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
public class Function160 extends FujitsuFunction {

    private static final Logger logger = getLogger(Function160.class.getName());

    @Override
    int getFunction() {
        return 0xa0;
    }

    /**
     * 0xa0    MFi4
     *
     * @param data see below
     * <pre>
     * 0    delta
     * 1    ff
     * 2    ff
     * 3-4  length
     * 5    vendor
     * 6    0xa0
     * 7    value, 1 ~ 5 in the corpus
     * </pre>
     */
    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.value = data[7] & 0xff;
logger.log(Level.DEBUG, "sound source setting: " + value);
    }

    /** 1 ~ 5 in the corpus */
    private int value;

    /** 1 ~ 5 in the corpus */
    public int getValue() {
        return value;
    }

    /** */
    public void setValue(int value) {
        this.value = value & 0xff;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[3];
        tmp[0] = (byte) (getVendor() | CARRIER_DOCOMO);
        tmp[1] = (byte) 0xa0;
        tmp[2] = (byte) value;
        return tmp;
    }
}
