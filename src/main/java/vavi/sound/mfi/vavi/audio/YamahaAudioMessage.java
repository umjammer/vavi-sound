/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.audio;

import java.util.logging.Level;

import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.mfi.vavi.track.MasterVolumeMessage;
import vavi.sound.mfi.vavi.track.TempoMessage;
import vavi.util.Debug;


/**
 * YamahaAudioMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070427 nsano initial version <br>
 */
public class YamahaAudioMessage extends MachineDependentMessage {

    /** */
    protected static final int maxMasterVolume = 0x7f;

    /** */
    protected static final int maxAdpcmVolume = 0x3f;

    /**
     *
     * @param masterVolume in %
     */
    public static MfiEvent getMasterVolumeEvent(int masterVolume) {
        int realMasterVolume = (int) (masterVolume * maxMasterVolume / 100f);
        MfiMessage message = new MasterVolumeMessage(0x00, 0xff, 0xb0, realMasterVolume);
        return new MfiEvent(message, 0L);
    }

    /** */
    private static final TempoMessage tempoMessage = new TempoMessage(0x00, 0xff, 0xcb, 100);

    /** @return TempoMessage is always the same instance */
    public static MfiEvent getTempoEvent() {
        return new MfiEvent(tempoMessage, 0L);
    }

    /** */
    public static int getDelta(float time) {
        float aDelta = (60f / tempoMessage.getTempo()) / tempoMessage.getTimeBase();
Debug.println(Level.FINE, "a delta: " + aDelta + ", tempo: " + tempoMessage.getTempo() + ", " + tempoMessage.getTimeBase());
        return Math.round(time / aDelta);
    }
}
