/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mfi5;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;

import static java.lang.System.getLogger;
import static vavi.sound.mfi.vavi.sequencer.MachineDependentFunction.CARRIER_DOCOMO;


/**
 * MFi 5 System exclusive message sequencer.
 * <p>
 * The vendor byte {@code 0x01} is no vendor ({@code 0x00}) and the DoCoMo carrier: the
 * messages of the carrier wide {@code MFi5PlugIn_DoCoMo} writer (its {@code supt} sub
 * chunk, {@code vers} 0500), which every maker's MFi 5 phone reads. Of the 311 files of
 * that writer in the corpus at {@code ~/Public/np2/mfi}, the 191 that have a machine
 * dependent message have them all under {@code 0x01} and no other.
 * </p>
 * <p>
 * "MFi 5" is not the name of a published spec, DoCoMo has none of that name: it is what
 * those files say of themselves, {@code vers} 0500 and the writer's name.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Mfi5Sequencer implements MachineDependentSequencer {

    private static final Logger logger = getLogger(Mfi5Sequencer.class.getName());

    /** no vendor, the carrier's own messages */
    static final int VENDOR_MFI5 = 0x00;

    @Override
    public int getId() {
        return VENDOR_MFI5 | CARRIER_DOCOMO;
    }

    /**
     *
     * @param data     mfi sysex
     * @param receiver
     */
    @Override
    public void sequence(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        int function = data[6] & 0xff;
logger.log(Level.TRACE, "function: 0x%02x".formatted(function));

        String key = VENDOR_MFI5 + "." + function;

        MachineDependentFunction mdf = MachineDependentFunction.Factory.getFunction(key);
        mdf.process(data, receiver);
    }
}
