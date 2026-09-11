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
 * Base of the messages whose level byte is 0x81 instead of 0x01.
 * <p>
 * TODO more investigation. Everything below is what the data says, not a spec.
 * </p>
 * <p>
 * Only one file out of the ~4400 in {@code ~/Public/np2/mfi} writes them
 * (`威風堂々　クラシカル.mld`, MFi 0301), 5 messages in all, so bit 7 of the level
 * byte may be a flag this one authoring tool sets, or simply a bad byte. What is
 * certain is that the rest of the message is a well formed level 0x01 message:
 * </p>
 * <pre>
 *  11 81 f0 04 02 04 30 00 79 87 ...   a 4 operator FM tone for bank 4, program 0x30
 *  11 81 f0 05 84 00 27 10 79 00 ...   a 10000Hz WT tone for bank 4 (drum), program 0
 *  11 81 f2 07 00 00 ...               16 bytes of channel status
 *  11 81 f3 01 00                      FM mode setting
 *  11 81 f3 03 08                      MaxGain setting
 * </pre>
 * <p>
 * So this masks bit 7 off and hands the message to the level 0x01 function.
 * The byte layout from the {@code f2} byte on is identical, and none of the
 * level 0x01 functions look at the level byte, so they decode it as is.
 * {@link NecSequencer} is deliberately left alone - a single file is not enough
 * to start masking the level byte for everybody.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
abstract class Level1AliasFunction implements MachineDependentFunction {

    /** the low nibble of the 0xf_ byte, which is what {@link NecSequencer} keys on */
    abstract int getFunction();

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "129_" + getFunction();
    }

    /**
     * @param message  see below
     *                 <pre>
     *                 0        delta
     *                 1        ff
     *                 2        ff
     *                 3-4      length
     *                 5        vendor
     *
     *                 6        81          level 0x01 with bit 7 set
     *                 7        f0 ~ f3
     *                 8        function
     *                 9~       as level 0x01
     *                 </pre>
     * @param receiver
     */
    @Override
    public void process(MachineDependentMessage message, Receiver receiver)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        if (data.length < 9) {
            throw new InvalidMfiDataException("too short for a level 0x81 message: " + data.length);
        }

        String key = VENDOR_NEC + "." + "1_" + (data[7] & 0xff) + "_" + (data[8] & 0x0f);

logger.log(Level.DEBUG, "level 0x81 (TODO more investigation), reading it as " + key);

        MachineDependentFunction.Factory.getFunction(key).process(message, receiver);
    }
}
