/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.panasonic;

import java.lang.System.Logger.Level;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;


/**
 * Panasonic System exclusive message function 0x94 processor.
 * (wave table voice block start)
 * <pre>
 * 0     delta
 * 1     ff
 * 2     ff
 * 3-4   length (0x0003)
 * 5     vendor
 * 6     0x94
 * 7     ? always 0
 * </pre>
 * <p>
 * The one message of the Panasonic MFi 4.0 plug in the Fujitsu one does not write. In the
 * corpus at {@code ~/Public/np2/mfi} all 153 of them (27 files) are {@code 94 00} at the
 * head of track 0, each right before the 0x90 ({@link Function144}) a wave table voice
 * block starts with, and a file that writes them writes exactly one per block. So it
 * marks where a voice starts; what its byte would say other than 0 is not known.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function148 extends PanasonicFunction {

    @Override
    int getFunction() {
        return 0x94;
    }

    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.value = data.length > 7 ? data[7] & 0xff : 0;
logger.log(Level.DEBUG, "voice block start: " + value);
    }

    /** ? always 0 */
    private int value;

    /** ? always 0 */
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
        tmp[0] = (byte) (VENDOR_PANASONIC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x94;
        tmp[2] = (byte) value;
        return tmp;
    }
}
