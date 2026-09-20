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
 * Fujitsu System exclusive message function 0xa1 processor.
 * (a per voice setting)
 * <p>
 * One data byte, split {@code voice, 6 bit value} the way every other MFi per
 * voice byte is ({@link vavi.sound.mfi.vavi.track.VolumeMessage} and friends).
 * </p>
 * <p>
 * Only one file of the ~4400 at {@code ~/Public/np2/mfi} writes it, 11 times, but
 * where it writes them settles the split: each one sits right after the
 * {@link vavi.sound.mfi.vavi.track.ChangeBankMessage} /
 * {@link vavi.sound.mfi.vavi.track.ChangeVoiceMessage} pair of one voice and its
 * top two bits are that voice's number, counting 0, 1, 2, 3 along with them
 * (0x00, 0x40, 0x80, 0xc0 - and 0x00, 0x50, 0x90 in the third round, that is the
 * same voices with the value 0x10 instead of 0).
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
public class Function161 extends FujitsuFunction {

    private static final Logger logger = getLogger(Function161.class.getName());

    @Override
    int getFunction() {
        return 0xa1;
    }

    /**
     * 0xa1    MFi4
     *
     * @param data see below
     * <pre>
     * 0    delta
     * 1    ff
     * 2    ff
     * 3-4  length
     * 5    vendor
     * 6    0xa1
     * 7    VVDDDDDD
     *        VV: voice
     *        DDDDDD: value, 0x00 or 0x10 in the corpus
     * </pre>
     */
    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.voice = (data[7] & 0xc0) >> 6;
        this.value =  data[7] & 0x3f;
logger.log(Level.DEBUG, "voice setting: %dvo %02x".formatted(voice, value));
    }

    /** 0 ~ 3 */
    private int voice;
    /** 0 ~ 63 */
    private int value;

    /** 0 ~ 3 */
    public int getVoice() {
        return voice;
    }

    /** 0 ~ 63 */
    public int getValue() {
        return value;
    }

    /** */
    public void setVoice(int voice) {
        this.voice = voice & 0x03;
    }

    /** */
    public void setValue(int value) {
        this.value = value & 0x3f;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[3];
        tmp[0] = (byte) (VENDOR_FUJITSU | CARRIER_DOCOMO);
        tmp[1] = (byte) 0xa1;
        tmp[2] = (byte) ((voice << 6) | value);
        return tmp;
    }
}
