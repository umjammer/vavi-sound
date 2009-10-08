/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mitsubishi;


import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependFunction;
import vavi.sound.mfi.vavi.sequencer.MachineDependFunctionFactory;
import vavi.sound.mfi.vavi.sequencer.MachineDependSequencer;
import vavi.sound.mfi.vavi.track.MachineDependMessage;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.FuetrekAudioEngine;


/**
 * Mitsubishi System exclusive message sequencer.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030618 nsano initial version <br>
 *          0.01 030711 nsano completes <br>
 *          0.02 030712 nsano implements voice part <br>
 */
public class MitsubishiSequencer implements MachineDependSequencer {

    /**
     *
     * @param message see below
     */
    public void sequence(MachineDependMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();
        int function = data[6] & 0xff;
//Debug.println("function: 0x" + StringUtil.toHex2(function));

        String key = MachineDependFunctionFactory.KEY_HEADER + function;
        MachineDependFunction mdf = factory.getFunction(key);
        mdf.process(message);
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
    private static MachineDependFunctionFactory factory = new MachineDependFunctionFactory("/vavi/sound/mfi/vavi/mitsubishi/mitsubishi.properties");
}

/* */
