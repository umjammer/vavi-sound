/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.panasonic;

import java.lang.System.Logger.Level;
import java.util.Arrays;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.util.StringUtil;


/**
 * Panasonic System exclusive message function 0xf0 processor.
 * (? unidentified)
 * <p>
 * Two messages in the corpus at {@code ~/Public/np2/mfi}, both {@code f0 80}, in two
 * files of {@code Ringtones from Cami P901iS} that have no {@code supt} sub chunk. Not
 * enough to name anything; the payload is kept as it is so that nothing Panasonic falls
 * through to {@link vavi.sound.mfi.vavi.sequencer.UndefinedFunction}.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function240 extends PanasonicFunction {

    @Override
    int getFunction() {
        return 0xf0;
    }

    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.data = Arrays.copyOfRange(data, 7, data.length);
logger.log(Level.DEBUG, "panasonic 0xf0: " + StringUtil.getDump(this.data));
    }

    /** the payload after the function byte */
    private byte[] data = new byte[0];

    /** the payload after the function byte as it is */
    public byte[] getData() {
        return data;
    }

    /** */
    public void setData(byte[] data) {
        this.data = data;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[2 + data.length];
        tmp[0] = (byte) (VENDOR_PANASONIC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0xf0;
        System.arraycopy(data, 0, tmp, 2, data.length);
        return tmp;
    }
}
