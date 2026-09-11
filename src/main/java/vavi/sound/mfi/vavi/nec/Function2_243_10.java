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
 * NEC System exclusive message function 0x02, 0xf3, 0x0a processor.
 * <p>
 * TODO what it means is unknown. Every MA-7 file written by SCP-MA7-N starts
 * track 0 with it and the value is always 1, so it looks like a "this is an
 * MA-7 score" declaration. It is decoded here so that it stops being an
 * undefined function.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
public class Function2_243_10 implements MachineDependentFunction {

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "2_243_10";
    }

    /**
     * 0x02, 0xf3, 0x0a (unknown)
     *
     * @param message  see below
     *                 <pre>
     *                 0        delta
     *                 1        ff
     *                 2        ff
     *                 3-4      length
     *                 5        vendor
     *
     *                 6        02
     *                 7        f3
     *                 8        ....1010
     *                              ~~~~
     *                              +------ 0xa
     *
     *                 9        always 1 so far
     *                 </pre>
     * @param receiver
     */
    @Override
    public void process(MachineDependentMessage message, Receiver receiver)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.value = data[9] & 0xff;

logger.log(Level.DEBUG, "MA-7 mode?: " + value);
    }

    /** always 1 so far */
    private int value = 1;

    /** */
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
        tmp[1] = (byte) 0x02;
        tmp[2] = (byte) 0xf3;
        tmp[3] = (byte) 0x0a;
        tmp[4] = (byte) value;
        return tmp;
    }
}
