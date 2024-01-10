/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.mfi.type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.vavi.sharp.SharpMessage;
import vavi.sound.sampled.mfi.MachineDependentMfiWithVoiceMaker;


/**
 * sh252.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050403 nsano initial version <br>
 */
public class sh252 implements MachineDependentMfiWithVoiceMaker {

    /**
     * @param data PCM, 16bit
     * @param sampleRate 4k, 8k
     * @param masterVolume 100% currently recommended
     * @param adpcmVolume 100% currently recommended
     */
    @Override
    public List<MfiEvent> getEvents(byte[] data, float time, int sampleRate, int bits, int channels, int masterVolume, int adpcmVolume)
        throws InvalidMfiDataException, IOException {

        List<MfiEvent> events = new ArrayList<>();

        // master volume
        events.add(SharpMessage.getMasterVolumeEvent(masterVolume));

        // tempo
        events.add(SharpMessage.getTempoEvent(time, sampleRate));

        // 0x81 adpcm vol
        events.addAll(SharpMessage.getVolumeEvents(channels, adpcmVolume));

        // 0x82 adpcm pan
        events.addAll(SharpMessage.getPanEvents(channels));

        // 0x84 adpcm
        events.addAll(SharpMessage.getAdpcmEvents(data, time, sampleRate, bits, channels, true));

        return events;
    }
}

/* */
