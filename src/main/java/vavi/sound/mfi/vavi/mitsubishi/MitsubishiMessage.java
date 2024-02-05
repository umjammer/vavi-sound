/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mitsubishi;

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
 * Mitsubishi System exclusive message.
 * <p>
 * TODO 機能ごとに FunctionXXX に移す
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030711 nsano initial version <br>
 *          0.01 030712 nsano fix sampling rate value for 16kHz <br>
 *          0.02 030901 nsano complete <br>
 */
public final class MitsubishiMessage extends FuetrekAudioMessage {

    /** wave length, use 0x83, 4bit in ADPCM, mono */
    private static final int MAX_BLOCK = 0xffff - Function131.HEADER_LENGTH;

    /** */
    private static final int L = 0;
    /** */
    private static final int R = 1;

    /**
     * 0x83
     * <p>
     * TODO 時間指定ない場合
     * </p>
     * @param pcm wave (PCM), 16bit
     * @param sampleRate 4k, 8k, 16k, 32k are available
     * @param bits adcpm bits
     * @param channels adcpm channels
     */
    public static List<MfiEvent> getAdpcmEvents(byte[] pcm, float time, int sampleRate, int bits, int channels) throws InvalidMfiDataException {
        int delta = getDelta(time);
Debug.println(Level.FINE, "delta: " + delta);
        AudioEngine audioEngine = MitsubishiSequencer.getAudioEngine();
        byte[] adpcm = audioEngine.encode(bits, channels, pcm);
Debug.println(Level.FINE, "adpcm mono length: " + adpcm.length);
        List<MfiEvent> events = new ArrayList<>();
        if (channels == 1) {
Debug.println(Level.FINE, "adpcm length: " + adpcm.length);
            events.addAll(getAdpcmEventsSub(L, 0, sampleRate, bits, adpcm));
            // 0x83 adpcm recycle
            events.add(getWaveEvent(L, 0, sampleRate, bits));
        } else {
            byte[] temp = new byte[adpcm.length / 2];
Debug.println(Level.FINE, "adpcm L, R length: " + temp.length);
            System.arraycopy(adpcm, 0, temp, 0, adpcm.length / 2);
            events.addAll(getAdpcmEventsSub(L, 0, sampleRate, bits, temp));
            System.arraycopy(adpcm, adpcm.length / 2, temp, 0, adpcm.length / 2);
            events.addAll(getAdpcmEventsSub(R, 1, sampleRate, bits, temp));
            // 0x83 adpcm recycle
            events.add(getWaveEvent(L, 0, sampleRate, bits));
            // 0x83 adpcm recycle
            events.add(getWaveEvent(R, 1, sampleRate, bits));
        }
        // fill nop
        for (int i = 0; i < delta / Nop2Message.maxDelta; i++) {
            Nop2Message nop2 = new Nop2Message(0xff, 0xff);
            events.add(new MfiEvent(nop2, 0L));
        }
        int moduloOfDelta = delta % Nop2Message.maxDelta;
        Nop2Message nop2 = new Nop2Message(moduloOfDelta % 0x100, moduloOfDelta / 0x100);
        events.add(new MfiEvent(nop2, 0L));

        return events;
    }

    /**
     * 0x83 sub
     * @param packetId TODO
     */
    private static List<MfiEvent> getAdpcmEventsSub(int channel, int packetId, int sampleRate, int bits, byte[] adpcm) throws InvalidMfiDataException {
        int numberOfChunks = adpcm.length / MAX_BLOCK;
        int moduloOfChunks = adpcm.length % MAX_BLOCK;

        List<MfiEvent> events = new ArrayList<>();

        for (int i = 0; i < numberOfChunks; i++) {
            byte[] chunk = new byte[MAX_BLOCK];
            System.arraycopy(adpcm, MAX_BLOCK * i, chunk, 0, MAX_BLOCK);
Debug.println(Level.FINE, "wave chunk(" + i + "): " + chunk.length);

            // 0x83 adpcm data
            events.add(getWaveEvent(channel, packetId, sampleRate, bits, !(i == (numberOfChunks - 1) && moduloOfChunks == 0), chunk));
        }
        if (moduloOfChunks != 0) {
            byte[] chunk = new byte[moduloOfChunks];
            System.arraycopy(adpcm, MAX_BLOCK * numberOfChunks, chunk, 0, moduloOfChunks);
Debug.println(Level.FINE, "wave chunk(" + numberOfChunks + "): " + chunk.length);

            // 0x83 adpcm data
            events.add(getWaveEvent(channel, packetId, sampleRate, bits, false, chunk));
        }

        return events;
    }

    /** 0x83 store */
    private static MfiEvent getWaveEvent(int channel, int packetId, int sampleRate, int bits, boolean continued, byte[] adpcm) throws InvalidMfiDataException {
        MachineDependentMessage message = new MitsubishiMessage();
        Function131 function = new Function131();
        function.setChannel(channel);
        function.setPacketId(packetId);
        function.setMode(Function132.MODE_STORE);
        function.setSamplingRate(sampleRate);
        function.setSamplingBits(bits);
        function.setContinued(continued);
        function.setAdpcm(adpcm);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    /**
     * 0x83 recycle
     * @param sampleRate 4k, 8k, 16k, 32k are available
     * @param bits 4, 2
     */
    private static MfiEvent getWaveEvent(int channel, int packetId, int sampleRate, int bits) throws InvalidMfiDataException {
        MachineDependentMessage message = new MitsubishiMessage();
        Function131 function = new Function131();
        function.setChannel(channel);
        function.setPacketId(packetId);
        function.setMode(Function132.MODE_RECYCLE);
        function.setSamplingRate(sampleRate);
        function.setSamplingBits(bits);
        function.setContinued(false);
        function.setAdpcm(new byte[0]);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    /** 0x84 store (unused) */
    public static MfiEvent getWaveEvent(int channel, int packetId, int sampleRate, int bits, boolean continued, int length, byte[] adpcm) throws InvalidMfiDataException {
        MachineDependentMessage message = new MitsubishiMessage();
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
        return new MfiEvent(message, 0L);
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

    /** 0x81 */
    private static MfiEvent getVolumeEvent(int channel, int realAdpcmVolume) throws InvalidMfiDataException {
        MachineDependentMessage message = new MitsubishiMessage();
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
            events.add(getPanpotEvent(L, 0x20));
        } else {
            events.add(getPanpotEvent(L, 0x00));
            events.add(getPanpotEvent(R, 0x3f));
        }
        return events;
    }

    /** 0x82 */
    private static MfiEvent getPanpotEvent(int channel, int pan) throws InvalidMfiDataException {
        MachineDependentMessage message = new MitsubishiMessage();
        Function130 function = new Function130();
        function.setChannel(channel);
        function.setPanpot(pan);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    /** 0x8f */
    public static MfiEvent getSettingEvent(int sampleRate, int bits, int channels) throws InvalidMfiDataException {
        MachineDependentMessage message = new MitsubishiMessage();
        Function143 function = new Function143();
        // 0x81
        function.setMaxSample(sampleRate);
        // 0x89
        function.setMaxSampleCue(sampleRate);
        // 0x83 要は 16kHz, 4bit mono の時 = 4, 8kHz, 4bit mono の時に 2
        function.setMaxParallel(sampleRate * (bits / 2) * channels); // TODO bit 適当
        // 0x8B 要は 16kHz, 4bit mono の時 = 4, 8kHz, 4bit mono の時に 2
        function.setMaxParallelCue(sampleRate * (bits / 2) * channels); // TODO bit 適当
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }
}
