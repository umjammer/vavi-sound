/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;

import static vavi.sound.mfi.vavi.nec.NecSequencer.VENDOR_NEC;


/**
 * NEC System exclusive message function 0x02, 0xf3, 0x0b processor.
 * (SfxChange, selects one of the effect blocks sent by
 * {@link Function2_240_14} / {@link Function2_240_15})
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
public class Function2_243_11 implements MachineDependentFunction {

    /** the converter rejects everything else */
    private static boolean isValidId(int id) {
        return id < 0x20 || (id >= 0x40 && id < 0x60);
    }

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "2_243_11";
    }

    /**
     * 0x02, 0xf3, 0x0b SfxChange
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
     * 7        f3
     * 8        ....1011
     *              ~~~~
     *              +------ 0xb
     *
     * 9        effect id, 0x00 ~ 0x1f or 0x40 ~ 0x5f
     * 10       f7
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.sfxId = data[9] & 0xff;

        if (!isValidId(sfxId)) {
logger.log(Level.WARNING, "SfxChange id is out of range (0 ~ 31, 64 ~ 95): " + sfxId);
        }

logger.log(Level.DEBUG, "SfxChange: " + sfxId);
    }

    /** 0 ~ 31 or 64 ~ 95 */
    private int sfxId;

    /** 0 ~ 31 or 64 ~ 95 */
    public int getSfxId() {
        return sfxId;
    }

    /** 0 ~ 31 or 64 ~ 95 */
    public void setSfxId(int sfxId) {
        if (!isValidId(sfxId)) {
            throw new IllegalArgumentException("sfx id is out of range (0 ~ 31, 64 ~ 95): " + sfxId);
        }
        this.sfxId = sfxId;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[6];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x02;
        tmp[2] = (byte) 0xf3;
        tmp[3] = (byte) 0x0b;
        tmp[4] = (byte) sfxId;
        tmp[5] = (byte) 0xf7;
        return tmp;
    }
}
