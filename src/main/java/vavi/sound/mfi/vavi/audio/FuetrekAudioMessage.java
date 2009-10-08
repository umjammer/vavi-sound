/*
 * Copyright (c) 2007 by KLab Inc., All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.audio;

import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.vavi.track.CuePointMessage;
import vavi.sound.mfi.vavi.track.MachineDependMessage;
import vavi.sound.mfi.vavi.track.MasterVolumeMessage;
import vavi.sound.mfi.vavi.track.TempoMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * FuetrekAudioMessage. 
 *
 * @author <a href="mailto:sano-n@klab.org">Naohide Sano</a> (nsano)
 * @version 0.00 070427 nsano initial version <br>
 */
public class FuetrekAudioMessage extends MachineDependMessage {

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
        return new MfiEvent(message, 0l);
    }

    /**
     * 
     * @param start true: start, false: end
     */
    public static MfiEvent getCuePointEvent(boolean start) {
        MfiMessage message = new CuePointMessage(0x00, start ? 0x00 : 0x01);
        return new MfiEvent(message, 0l);
    }

    /** */
    public static MfiEvent getTempoEvent(float time, int sampleRate) {
        TempoMessage message = getTempoMessage(time, sampleRate);
        return new MfiEvent(message, 0l);
    }

    /** */
    private static TempoMessage getTempoMessage(float time, int sampleRate) {
        int baseTempo = 127 / (sampleRate / 8000);
        int baseTimeBase = 0xc3;
        float aDelta;
        TempoMessage message;
        while (true) {
Debug.println("tempo: " + baseTempo + ", timeBase: 0x" + StringUtil.toHex2(baseTimeBase));
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
Debug.println("over limit");
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
Debug.println("a delta: " + aDelta + ", tempo: " + message.getTempo() + ", " + message.getTimeBase());
        return Math.round(time / aDelta);
    }

    private static final TempoMessage tempoMessageOld = new TempoMessage(0x00, 0xff, 0xc2, 50);

    /**
     * older version
     * @return TempoMessage is always the same instance
     */
    public static MfiEvent getTempoEvent() {
        return new MfiEvent(tempoMessageOld, 0l);
    }

    /** older version */
    public static int getDelta(float time) {
        float aDelta = (60f / tempoMessageOld.getTempo()) / tempoMessageOld.getTimeBase();
Debug.println("a delta: " + aDelta + ", tempo: " + tempoMessageOld.getTempo() + ", " + tempoMessageOld.getTimeBase());
        return Math.round(time / aDelta);
    }
}

/* */
