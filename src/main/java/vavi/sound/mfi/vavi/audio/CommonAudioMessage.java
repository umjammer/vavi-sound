/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.audio;

import java.util.ArrayList;
import java.util.List;

import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.vavi.AudioDataMessage;
import vavi.sound.mfi.vavi.header.AinfMessage;
import vavi.sound.mfi.vavi.header.ExstMessage;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer.Factory;
import vavi.sound.mfi.vavi.track.AudioChannelPanpotMessage;
import vavi.sound.mfi.vavi.track.AudioChannelVolumeMessage;
import vavi.sound.mfi.vavi.track.AudioPlayMessage;
import vavi.sound.mfi.vavi.track.CuePointMessage;
import vavi.sound.mfi.vavi.track.MasterVolumeMessage;
import vavi.sound.mfi.vavi.track.Nop2Message;
import vavi.sound.mfi.vavi.track.TempoMessage;
import vavi.sound.mobile.AudioEngine;
import vavi.util.Debug;


/**
 * CommonMessage. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public class CommonAudioMessage {

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
     * @param adpcmVolume in %
     */
    public static MfiEvent getVolumeEvent(int adpcmVolume) {
        int realAdpcmVolume = (int) (adpcmVolume * maxAdpcmVolume / 100f);
        MfiMessage message = new AudioChannelVolumeMessage(0x00, 0x7f, 0x80, realAdpcmVolume);
        return new MfiEvent(message, 0l);
    }

    /**
     * 
     * @param pan
     */
    public static MfiEvent getPanEvent(int pan) {
        MfiMessage message = new AudioChannelPanpotMessage(0x00, 0x7f, 0x81, pan);
        return new MfiEvent(message, 0l);
    }

    /** */
    public static MfiEvent getPlayEvent() {
        MfiMessage message = new AudioPlayMessage(0x00, 0x7f, 0x00, new byte[] { 0, 63 });
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
    private static final TempoMessage tempoMessage = new TempoMessage(0x00, 0xff, 0xcb, 100);

    /** @return TempoMessage is always the same instance */
    public static MfiEvent getTempoEvent() {
        return new MfiEvent(tempoMessage, 0l);
    }

    /** */
    private static int getDelta(float time) {
        float aDelta = (60f / tempoMessage.getTempo()) / tempoMessage.getTimeBase();
Debug.println("a delta: " + aDelta + ", tempo: " + tempoMessage.getTempo() + ", " + tempoMessage.getTimeBase());
        return Math.round(time / aDelta);
    }

    /** */
    public static List<MfiEvent> getNop2Events(float time) {
        List<MfiEvent> events = new ArrayList<MfiEvent>();

        int delta = getDelta(time);
Debug.println("delta: " + delta);
        for (int i = 0; i < delta / Nop2Message.maxDelta; i++) {
            events.add(new MfiEvent(new Nop2Message(0xff, 0xff), 0l));
        }
        int moduloOfDelta = delta % Nop2Message.maxDelta;
        events.add(new MfiEvent(new Nop2Message(moduloOfDelta % 0x100, moduloOfDelta / 0x100), 0l));

        return events;
    }

    /** */
    public static List<MfiEvent> getAudioEvents(int format, byte[] data, int sampleRate, int bits, int channels) {
        List<MfiEvent> events = null;
        switch (format) {
        case 0x80:
            events = getAudioEventsType1(data, sampleRate, bits, channels);
            break;
        case 0x81:
            events = getAudioEventsType2(data, sampleRate, bits, channels);
            break;
        default:
            throw new IllegalArgumentException("undefined format: " + format);
        }
        return events;
    }

    /** 0x80 */
    private static List<MfiEvent> getAudioEventsType1(byte[] data, int sampleRate, int bits, int channels) {
        List<MfiEvent> events = new ArrayList<MfiEvent>();
        // exst
        events.add(new MfiEvent(new ExstMessage(1), 0l));

        // ainf
        events.add(new MfiEvent(new AinfMessage(false, 1), 0l));

        // audio data
        AudioEngine audioEngine = Factory.getAudioEngine(0x80); // TODO 0x80 is not defined in spec
        byte[] adpcm = audioEngine.encode(bits, channels, data);
        AdpmMessage adpmMessage = new AdpmMessage(sampleRate / 1000, bits, false, channels);
        AudioDataMessage audioData = new AudioDataMessage(0x80, 0x00, adpmMessage); // TODO 0x80 is not defined in spec
        audioData.setData(adpcm);
        events.add(new MfiEvent(audioData, 0l));

        return events;
    }

    /** 0x81 */
    private static List<MfiEvent> getAudioEventsType2(byte[] data, int sampleRate, int bits, int channels) {
        List<MfiEvent> events = new ArrayList<MfiEvent>();

        // exst
        events.add(new MfiEvent(new ExstMessage(1), 0l));

        // ainf
        events.add(new MfiEvent(new AinfMessage(false, 1, new AinfMessage.AudioInfo(0x81, new byte[] { 0x10, 0x08, 0x10, 0x10, 0x08, 0x10 })), 0l));

        // audio data
        AudioEngine audioEngine = Factory.getAudioEngine(0x81);
        byte[] adpcm = audioEngine.encode(bits, channels, data);
        AdpmMessage adpmMessage = new AdpmMessage(sampleRate / 1000, bits, false, channels);
        AudioDataMessage audioData = new AudioDataMessage(AudioDataMessage.FORMAT_ADPCM_TYPE2, 0x00, adpmMessage);
        audioData.setData(adpcm);
        events.add(new MfiEvent(audioData, 0l));

        return events;
    }
}

/* */
