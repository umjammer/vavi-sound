/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sharp;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.FuetrekAudioEngine;

import static java.lang.System.getLogger;


/**
 * Sharp System exclusive message sequencer.
 * <pre>
 * properties file ... "/vavi/sound/mfi/vavi/sharp/sharp.properties"
 * name prefix ... "function."
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051111 nsano initial version <br>
 */
public class SharpSequencer implements MachineDependentSequencer {

    private static final Logger logger = getLogger(SharpSequencer.class.getName());

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

        MachineDependentFunction mdf = factory.getFunction(String.valueOf(function));
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

    // ----

    /** */
    private static final MachineDependentFunction.Factory factory =
            new MachineDependentFunction.Factory("/vavi/sound/mfi/vavi/sharp/sharp.properties");
}
