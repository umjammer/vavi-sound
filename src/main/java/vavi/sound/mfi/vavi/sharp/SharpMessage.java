/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sharp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.vavi.audio.FuetrekAudioMessage;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.mfi.vavi.track.Nop2Message;
import vavi.sound.mobile.AudioEngine;
import vavi.util.Debug;


/**
 * Sharp System exclusive message.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051111 nsano initial version <br>
 */
public class SharpMessage extends FuetrekAudioMessage {

    /** wave length, use 0x84, 4bit in ADPCM, mono */
    private static final int MAX_BLOCK = 0xffff - Function132.HEADER_LENGTH;

    /** */
    private static final int L = 0;
    /** */
    private static final int R = 1;

    /**
     * whole adpcm, use 0x84
     * @param pcm pcm data
     * @param sampleRate adpcm sampling rate
     * @param bits adpcm bits
     * @param channels adpcm channels
     * @return mfi events
     */
    public static List<MfiEvent> getAdpcmEvents(byte[] pcm, float time, int sampleRate, int bits, int channels, boolean fillNop) throws InvalidMfiDataException {
        int delta = getDelta(time, sampleRate);
Debug.println(Level.FINE, "delta: " + delta);
        AudioEngine audioEngine = SharpSequencer.getAudioEngine();
        byte[] adpcm = audioEngine.encode(bits, channels, pcm);
        List<MfiEvent> events = new ArrayList<>();
        if (channels == 1) {
Debug.println(Level.FINE, "adpcm mono length: " + adpcm.length);
            events.addAll(getAdpcmEventsSub(L, 0, sampleRate, bits, adpcm));
            // 0x84 adpcm recycle
            events.add(getWaveEvent(L, 0, sampleRate, bits, adpcm.length));
        } else {
            byte[] temp = new byte[adpcm.length / 2];
Debug.println(Level.FINE, "adpcm L, R length: " + temp.length);
            System.arraycopy(adpcm, 0, temp, 0, adpcm.length / 2);
            events.addAll(getAdpcmEventsSub(L, 0, sampleRate, bits, temp));
            System.arraycopy(adpcm, adpcm.length / 2, temp, 0, adpcm.length / 2);
            events.addAll(getAdpcmEventsSub(R, 1, sampleRate, bits, temp));
            // 0x84 adpcm recycle
            events.add(getWaveEvent(L, 0, sampleRate, bits, temp.length));
            // 0x84 adpcm recycle
            events.add(getWaveEvent(R, 1, sampleRate, bits, temp.length));
        }
        // fill nop
        if (fillNop) {
            for (int i = 0; i < delta / Nop2Message.maxDelta; i++) {
                Nop2Message nop2 = new Nop2Message(0xff, 0xff);
                events.add(new MfiEvent(nop2, 0l));
            }
            int moduloOfDelta = delta % Nop2Message.maxDelta;
            Nop2Message nop2 = new Nop2Message(moduloOfDelta % 0x100, moduloOfDelta / 0x100);
            events.add(new MfiEvent(nop2, 0l));
        }
        return events;
    }

    /** 0x84 sub */
    private static List<MfiEvent> getAdpcmEventsSub(int channel, int packetId, int sampleRate, int bits, byte[] adpcm) throws InvalidMfiDataException {
        int numberOfChunks = adpcm.length / MAX_BLOCK;
        int moduloOfChunks = adpcm.length % MAX_BLOCK;

        List<MfiEvent> events = new ArrayList<>();

        for (int i = 0; i < numberOfChunks; i++) {
            byte[] chunk = new byte[MAX_BLOCK];
            System.arraycopy(adpcm, MAX_BLOCK * i, chunk, 0, MAX_BLOCK);
Debug.println(Level.FINE, "wave chunk(" + i + "): " + chunk.length);

            // 0x84 adpcm data
            events.add(getWaveEvent(channel, packetId, sampleRate, bits, !(i == (numberOfChunks - 1) && moduloOfChunks == 0), i == 0 ? adpcm.length : 0, chunk));
        }
        if (moduloOfChunks != 0) {
            byte[] chunk = new byte[moduloOfChunks];
            System.arraycopy(adpcm, MAX_BLOCK * numberOfChunks, chunk, 0, moduloOfChunks);
Debug.println(Level.FINE, "wave chunk(" + numberOfChunks + "): " + chunk.length);

            // 0x84 adpcm data
            events.add(getWaveEvent(channel, packetId, sampleRate, bits, false, numberOfChunks == 0 ? adpcm.length : 0, chunk));
        }

        return events;
    }

    /**
     * 0x84 adpcm store
     * @param packetId packetId
     * @param sampleRate 4k, 8k, 16k, 32k are available
     * @param bits 4, 2
     * @param continued
     * @param adpcm wave (PCM), 16bit, mono
     */
    private static MfiEvent getWaveEvent(int channel, int packetId, int sampleRate, int bits, boolean continued, int length, byte[] adpcm) throws InvalidMfiDataException {
        MachineDependentMessage message = new SharpMessage();
        Function132 function = new Function132();
        function.setChannel(channel);
        function.setPacketId(packetId);
        function.setMode(Function132.MODE_STORE);
        function.setSamplingRate(sampleRate);
        function.setSamplingBits(bits);
        function.setContinued(continued);
        function.setLength(length);
        function.setAdpcm(adpcm);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0l);
    }

    /**
     * 0x84 adpcm recycle
     * @param packetId packeId
     * @param sampleRate 4k, 8k, 16k, 32k are available
     * @param bits 4, 2
     */
    private static MfiEvent getWaveEvent(int channel, int packetId, int sampleRate, int bits, int length) throws InvalidMfiDataException {
        MachineDependentMessage message = new SharpMessage();
        Function132 function = new Function132();
        function.setChannel(channel);
        function.setPacketId(packetId);
        function.setMode(Function132.MODE_RECYCLE);
        function.setSamplingRate(sampleRate);
        function.setSamplingBits(bits);
        function.setContinued(false);
        function.setLength(length);
        function.setAdpcm(new byte[0]);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0l);
    }

    /**
     * 0x81
     * @param adpcmVolume in %
     */
    public static List<MfiEvent> getVolumeEvents(int channels, int adpcmVolume) throws InvalidMfiDataException {
        List<MfiEvent> events = new ArrayList<>();
        int realAdpcmVolume = (int) (adpcmVolume * maxAdpcmVolume / 100f);
        if (channels == 1) {
            events.add(getVolumeEvent(L, realAdpcmVolume));
        } else {
            events.add(getVolumeEvent(L, realAdpcmVolume));
            events.add(getVolumeEvent(R, realAdpcmVolume));
        }
        return events;
    }

    /** 0x81 volume */
    private static MfiEvent getVolumeEvent(int channel, int realAdpcmVolume) throws InvalidMfiDataException {
        MachineDependentMessage message = new SharpMessage();
        Function129 function = new Function129();
        function.setChannel(channel);
        function.setVolume(realAdpcmVolume);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    /** 0x82 */
    public static List<MfiEvent> getPanEvents(int channels) throws InvalidMfiDataException {
        List<MfiEvent> events = new ArrayList<>();
        if (channels == 1) {
            events.add(getPanEvent(L, 0x20));
        } else {
            events.add(getPanEvent(L, 0x00));
            events.add(getPanEvent(R, 0x3f));
        }
        return events;
    }

    /** 0x82 pan */
    private static MfiEvent getPanEvent(int channel, int pan) throws InvalidMfiDataException {
        MachineDependentMessage message = new SharpMessage();
        Function130 function = new Function130();
        function.setChannel(channel);
        function.setPanpot(pan);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    /** 0x8f setting */
    public static MfiEvent getSettingEvent(int sampleRate, int bits, int channels) throws InvalidMfiDataException {
        MachineDependentMessage message = new SharpMessage();
        Function143 function = new Function143();
        // 0x81
        function.setMaxSample(sampleRate);
        // 0x89
        function.setMaxSampleCue(sampleRate);
        // 0x83 要は 16kHz, 4bit mono の時 = 4, 8kHz, 4bit mono の時に 2
        function.setMaxParallel(sampleRate * (sampleRate / 8000) * channels); // TODO 適当
        // 0x8B 要は 16kHz, 4bit mono の時 = 4, 8kHz, 4bit mono の時に 2
        function.setMaxParallelCue(sampleRate * (sampleRate / 8000) * channels); // TODO 適当
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }
}

/* */
