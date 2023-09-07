/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;


/**
 * NEC System exclusive message function 0xf2, 0x_1 processor.
 * (ADPCM volume)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030827 nsano initial version <br>
 */
public class Function242_1 implements MachineDependentFunction {

    /**
     * 0xf2, 0x_1 ADPCM volume, length 4
     *
     * @param message see below
     * <pre>
     * 0    delta
     * 1    ff
     * 2    ff
     * 3-4  length
     * 5    vendor
     *
     * 6    f2
     * 7    _1
     * 8    volume
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.volume = data[8] & 0x3f;                   //

        // 8 volume
Debug.println(Level.FINE, "ADPCM volume: " + volume);
    }

    /** */
    private int volume;

    /** */
    public void setVolume(int volume) {
        this.volume = volume & 0x3f;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[4];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0xf2;                           // f2
        tmp[2] = (byte) 0x01;                           // channel
        tmp[3] = (byte) volume;
        return tmp;
    }
}

/* */
