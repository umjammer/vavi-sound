/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mitsubishi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.FuetrekAudioEngine;

import static java.lang.System.getLogger;
import static vavi.sound.mfi.vavi.sequencer.MachineDependentFunction.CARRIER_DOCOMO;


/**
 * Mitsubishi System exclusive message sequencer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030618 nsano initial version <br>
 *          0.01 030711 nsano completes <br>
 *          0.02 030712 nsano implements voice part <br>
 */
public class MitsubishiSequencer implements MachineDependentSequencer {

    private static final Logger logger = getLogger(MitsubishiSequencer.class.getName());

    static final int VENDOR_MITSUBISHI = 0x60; // D

    @Override
    public int getId() {
        return VENDOR_MITSUBISHI | CARRIER_DOCOMO;
    }

    /**
     *
     * @param message see below
     */
    @Override
    public void sequence(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();
        int function = data[6] & 0xff;
logger.log(Level.TRACE, "function: 0x%02x".formatted(function));

        String key = VENDOR_MITSUBISHI + "." + function;
        MachineDependentFunction mdf = MachineDependentFunction.Factory.getFunction(key);
        if (mdf != null) {
            mdf.process(message);
        } else {
logger.log(Level.WARNING, "unsupported function: 0x%02x".formatted(function));
        }
    }

    // ----

    /** */
    private static final AudioEngine player = new FuetrekAudioEngine();

    /** */
    static AudioEngine getAudioEngine() {
        return player;
    }
}
