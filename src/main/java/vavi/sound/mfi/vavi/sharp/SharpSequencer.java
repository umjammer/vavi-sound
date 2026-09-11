/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sharp;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.FuetrekAudioEngine;

import static java.lang.System.getLogger;
import static vavi.sound.mfi.vavi.sequencer.MachineDependentFunction.CARRIER_DOCOMO;


/**
 * Sharp System exclusive message sequencer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051111 nsano initial version <br>
 */
public class SharpSequencer implements MachineDependentSequencer {

    private static final Logger logger = getLogger(SharpSequencer.class.getName());

    static final int VENDOR_SHARP = 0x70; // SH

    @Override
    public int getId() {
        return VENDOR_SHARP | CARRIER_DOCOMO;
    }

    /**
     *
     * @param message  see below
     * @param receiver
     */
    @Override
    public void sequence(MachineDependentMessage message, Receiver receiver)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();
        int function = data[6] & 0xff;
logger.log(Level.TRACE, "function: 0x%02x".formatted(function));

        String key = VENDOR_SHARP + "." + function;

        MachineDependentFunction mdf = MachineDependentFunction.Factory.getFunction(key);
        mdf.process(message, receiver);
    }

    // ----

    /** */
    private static final AudioEngine player = new FuetrekAudioEngine();

    /** */
    static AudioEngine getAudioEngine() {
        return player;
    }
}
