/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.lang.System.Logger.Level;
import java.util.Arrays;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.StringUtil;

import static vavi.sound.mfi.vavi.nec.NecSequencer.VENDOR_NEC;


/**
 * Base of the MA-7 effect (SFX) parameter messages 0x02, 0xf0, 0x0e and 0x0f.
 * <p>
 * The payload is an array of {@link #BLOCK} byte blocks, at most
 * {@link #MAX_BLOCKS} of them, which is what the converter validates
 * ({@code 0 < length <= 1024 && length % 32 == 0}). A {@code SfxChange}
 * ({@link Function2_243_11}) selects one of the blocks. The meaning of the
 * parameters inside a block is not known yet.
 * </p>
 * <pre>
 * 0        delta
 * 1        ff
 * 2        ff
 * 3-4      length
 * 5        vendor
 *
 * 6        02
 * 7        f0
 * 8        ....111.
 *              ~~~~
 *              +------ 0xe or 0xf
 *
 * 9~       n * 32 bytes of effect parameters
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
abstract class EffectDataFunction implements MachineDependentFunction {

    /** size of one effect parameter block */
    public static final int BLOCK = 32;

    /** the converter refuses more than 1024 bytes */
    public static final int MAX_BLOCKS = 32;

    /** 0x0e or 0x0f */
    abstract int getFunction();

    /** for the log */
    abstract String getName();

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "2_240_" + getFunction();
    }

    @Override
    public void process(MachineDependentMessage message, Receiver receiver)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        setData(Arrays.copyOfRange(data, 9, data.length));

logger.log(Level.DEBUG, getName() + ": " + getBlockCount() + " block(s)");
logger.log(Level.TRACE, "effect:\n" + StringUtil.getDump(this.data));
    }

    /** {@link #getBlockCount()} * {@link #BLOCK} bytes */
    private byte[] data = new byte[0];

    /** {@link #getBlockCount()} * {@link #BLOCK} bytes */
    public byte[] getData() {
        return data;
    }

    /** */
    public void setData(byte[] data) throws InvalidMfiDataException {
        if (data.length == 0 || data.length % BLOCK != 0 || data.length / BLOCK > MAX_BLOCKS) {
            throw new InvalidMfiDataException("effect data must be 1 ~ " + MAX_BLOCKS + " blocks of " + BLOCK + " bytes: " + data.length);
        }
        this.data = data;
    }

    /** */
    public int getBlockCount() {
        return data.length / BLOCK;
    }

    /** */
    public byte[] getBlock(int index) {
        return Arrays.copyOfRange(data, index * BLOCK, (index + 1) * BLOCK);
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[4 + data.length];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x02;
        tmp[2] = (byte) 0xf0;
        tmp[3] = (byte) getFunction();

        System.arraycopy(data, 0, tmp, 4, data.length);

        return tmp;
    }
}
