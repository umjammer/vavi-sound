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
 * NEC System exclusive message function 0x02, 0xf3, 0x03 processor.
 * (MaxGain setting, the MFi 4.0 counterpart of {@link Function1_243_3})
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
public class Function2_243_3 implements MachineDependentFunction {

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "2_243_3";
    }

    /**
     * 0x02, 0xf3, 0x03 MaxGain Setting
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
     *                 8        ....0011
     *                              ~~~~
     *                              +------ 0x3
     *
     *                 9        maxGain 0x00 ~ 0x60 (-48db), default 0x18 (-12db)
     *                 10       0xf7 when the converter derived the gain from a curve, absent otherwise
     *                 </pre>
     * @param receiver
     */
    @Override
    public void process(MachineDependentMessage message, Receiver receiver)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.maxGain = data[9] & 0xff;

logger.log(Level.DEBUG, "MaxGain: " + maxGain);
    }

    /** 0 ~ 96 (-96db) */
    private int maxGain = 24; // -12db

    /** 0 ~ 96 (-96db) */
    public int getMaxGain() {
        return maxGain;
    }

    /** 0 ~ 96 (-96db), default 24 */
    public void setMaxGain(int maxGain) {
        this.maxGain = Math.min(maxGain, 96);
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[5];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x02;
        tmp[2] = (byte) 0xf3;
        tmp[3] = (byte) 0x03;
        tmp[4] = (byte) maxGain;
        return tmp;
    }
}
