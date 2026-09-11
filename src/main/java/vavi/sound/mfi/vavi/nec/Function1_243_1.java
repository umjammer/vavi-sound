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
 * NEC System exclusive message function 0x01, 0xf3, 0x01 processor.
 * (FM mode setting)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
public class Function1_243_1 implements MachineDependentFunction {

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "1_243_1";
    }

    /**
     * 0x01, 0xf3, 0x01 FM-Mode
     *
     * @param message see below
     * <pre>
     * 0        delta
     * 1        ff
     * 2        ff
     * 3-4      length
     * 5        vendor
     *
     * 6        01
     * 7        f3
     * 8        function 0x01
     * 9        0 or 1
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.value = data[9] & 0xff;

logger.log(Level.DEBUG, "FM-Mode: " + value);
    }

    /** 0 or 1 */
    private int value;

    /** 0 or 1 */
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

        byte[] tmp = new byte[5];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) 0xf3;
        tmp[3] = (byte) 0x01;
        tmp[4] = (byte) value;
        return tmp;
    }
}
