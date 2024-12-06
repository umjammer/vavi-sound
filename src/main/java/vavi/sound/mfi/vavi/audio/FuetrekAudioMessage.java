/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.audio;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.vavi.track.CuePointMessage;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.mfi.vavi.track.MasterVolumeMessage;
import vavi.sound.mfi.vavi.track.TempoMessage;

import static java.lang.System.getLogger;


/**
 * FuetrekAudioMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070427 nsano initial version <br>
 */
public class FuetrekAudioMessage extends MachineDependentMessage {

    private static final Logger logger = getLogger(FuetrekAudioMessage.class.getName());

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

    /**
     *
     * @param start true: start, false: end
     */
    public static MfiEvent getCuePointEvent(boolean start) {
        MfiMessage message = new CuePointMessage(0x00, start ? 0x00 : 0x01);
        return new MfiEvent(message, 0L);
    }

    /** */
    public static MfiEvent getTempoEvent(float time, int sampleRate) {
        TempoMessage message = getTempoMessage(time, sampleRate);
        return new MfiEvent(message, 0L);
    }

    /** */
    private static TempoMessage getTempoMessage(float time, int sampleRate) {
        int baseTempo = 127 / (sampleRate / 8000);
        int baseTimeBase = 0xc3;
        float aDelta;
        TempoMessage message;
        while (true) {
logger.log(Level.DEBUG, "tempo: %d, timeBase: 0x%02x".formatted(baseTempo, baseTimeBase));
            message = new TempoMessage(0x00, 0xff, baseTimeBase, baseTempo);
            aDelta = (60f / message.getTempo()) / message.getTimeBase();
            if (Math.round(time / aDelta) > 255) {
                if ((baseTempo / 2) > 15) {
                    baseTempo /= 2;
                } else {
                    if (baseTimeBase > 0xc0) {
                        baseTimeBase -= 1;
                    } else {
                        if ((baseTempo / 2) > 0) {
                            baseTempo /= 2;
                        } else {
logger.log(Level.INFO, "over limit");
                            break;
                        }
                    }
                }
            } else {
                break;
            }
        }
        return message;
    }

    /** */
    public static int getDelta(float time, int sampleRate) {
        TempoMessage message = getTempoMessage(time, sampleRate);
        float aDelta = (60f / message.getTempo()) / message.getTimeBase();
logger.log(Level.DEBUG, "a delta: " + aDelta + ", tempo: " + message.getTempo() + ", " + message.getTimeBase());
        return Math.round(time / aDelta);
    }

    private static final TempoMessage tempoMessageOld = new TempoMessage(0x00, 0xff, 0xc2, 50);

    /**
     * older version
     * @return TempoMessage is always the same instance
     */
    public static MfiEvent getTempoEvent() {
        return new MfiEvent(tempoMessageOld, 0L);
    }

    /** older version */
    public static int getDelta(float time) {
        float aDelta = (60f / tempoMessageOld.getTempo()) / tempoMessageOld.getTimeBase();
logger.log(Level.DEBUG, "a delta: " + aDelta + ", tempo: " + tempoMessageOld.getTempo() + ", " + tempoMessageOld.getTimeBase());
        return Math.round(time / aDelta);
    }
}
