/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mitsubishi;

import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.FuetrekAudioEngine;
import vavi.util.Debug;


/**
 * Mitsubishi System exclusive message sequencer.
 * <pre>
 * properties file ... "/vavi/sound/mfi/vavi/mitsubishi/mitsubishi.properties"
 * name prefix ... "function."
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030618 nsano initial version <br>
 *          0.01 030711 nsano completes <br>
 *          0.02 030712 nsano implements voice part <br>
 */
public class MitsubishiSequencer implements MachineDependentSequencer {

    /**
     *
     * @param message see below
     */
    @Override
    public void sequence(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();
        int function = data[6] & 0xff;
Debug.printf(Level.FINER, "function: 0x%02x", function);

        MachineDependentFunction mdf = factory.getFunction(String.valueOf(function));
        if (mdf != null) {
            mdf.process(message);
        } else {
Debug.printf(Level.WARNING, "unsupported function: 0x%02x", function);
        }
    }

    //-------------------------------------------------------------------------

    /** */
    private static AudioEngine player = new FuetrekAudioEngine();

    /** */
    static AudioEngine getAudioEngine() {
        return player;
    }

    //-------------------------------------------------------------------------

    /** */
    private static MachineDependentFunction.Factory factory =
            new MachineDependentFunction.Factory("/vavi/sound/mfi/vavi/mitsubishi/mitsubishi.properties");
}
