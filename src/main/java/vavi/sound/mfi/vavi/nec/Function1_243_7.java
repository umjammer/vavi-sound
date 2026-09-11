/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.lang.System.Logger.Level;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;

import static vavi.sound.mfi.vavi.nec.NecSequencer.VENDOR_NEC;


/**
 * NEC System exclusive message function 0x01, 0xf3, 0x07 processor.
 * (TODO unknown, one byte, 0 or 3 in the corpus)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
public class Function1_243_7 implements MachineDependentFunction {

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "1_243_7";
    }

    /**
     * 0x01, 0xf3, 0x07 Unknown(f3.07)
     *
     * @param message  see below
     *                 <pre>
     *                 0        delta
     *                 1        ff
     *                 2        ff
     *                 3-4      length
     *                 5        vendor
     *
     *                 6        01
     *                 7        f3
     *                 8        function 0x07
     *                 9        0 or 3
     *                 </pre>
     * @param receiver
     */
    @Override
    public void process(MachineDependentMessage message, Receiver receiver)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.value = data[9] & 0xff;

logger.log(Level.DEBUG, "Unknown(f3.07): " + value);
    }

    /** 0 or 3 */
    private int value;

    /** 0 or 3 */
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
        tmp[3] = (byte) 0x07;
        tmp[4] = (byte) value;
        return tmp;
    }
}
