/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.vavi.audio.YamahaAudioMessage;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.mfi.vavi.track.Nop2Message;
import vavi.sound.mobile.AudioEngine;
import vavi.util.Debug;


/**
 * NEC System exclusive message.
 * <li>TODO use StreamSlaveOn
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030711 nsano initial version <br>
 */
public class NecMessage extends YamahaAudioMessage {

    /** wave length, use (0x01, 0xf0, 0x07), 4bit in ADPCM, mono */
    private static int MAX_BLOCK = 0xffff - Function1_240_7.HEADER_LENGTH;

    /** */
    private static ThreadLocal<Integer> sequence = new ThreadLocal<>();

    /** thread local sequence */
    private static void initSequence() {
        if (sequence.get() == null) {
            sequence.set(0);
Debug.println(Level.FINER, "thread local sequence: init");
        }
    }

    /** thread local sequence */
    private static MfiEvent getStreamOn(int streamNumber, int velocity) throws InvalidMfiDataException {
        MfiEvent event = NecMessage.getStreamOnEvent(sequence.get() % 2, streamNumber, velocity);
        sequence.set(sequence.get() + 1);
        return event;
    }

    /** */
    private static int PCM_MAX_BLOCK = MAX_BLOCK * 4;

    /** */
    private static final int maxVelocity = 0x7f;

    /** */
    private static final int L = 0;
    /** */
    private static final int R = 1;

    /**
     * Creates adpcm data message. (wav2mld)
     * <pre>
     * 0x01 0xf0 0x_7
     * </pre>
     * @param pcm wave (PCM) data (4bit required)
     * @param sampleRate 4000 ~ 16000 are available
     * @param bits ignored, assumed 4
     */
    public static List<MfiEvent> getAdpcmEvents(byte[] pcm, float time, int sampleRate, int bits, int channels, int adpcmVolume) throws InvalidMfiDataException {
        int delta = getDelta(time);
Debug.println(Level.FINE, "delta: " + delta + ", time: " + time);
        int velocity = (int) (adpcmVolume * maxVelocity / 100f);

        AudioEngine audioEngine = NecSequencer.getAudioEngine();

        int numberOfChunks = pcm.length / (PCM_MAX_BLOCK * channels);
        int moduloOfChunks = pcm.length % (PCM_MAX_BLOCK * channels);

        List<MfiEvent> events = new ArrayList<>();

        // 1. data
        int streamNumber = 0;
        for (int i = 0; i < numberOfChunks; i++) {
            byte[] temp = new byte[PCM_MAX_BLOCK * channels];
            System.arraycopy(pcm, (PCM_MAX_BLOCK * channels) * i, temp, 0, PCM_MAX_BLOCK * channels);
            byte[] chunk = audioEngine.encode(4, channels, temp);
            if (channels == 1) {
Debug.println(Level.FINE, "wave chunk(" + i + "): " + chunk.length);

                // adpcm data
                events.add(getVoiceEvent(streamNumber++, channels, sampleRate, chunk));

            } else {
                byte[] chunkM = new byte[chunk.length / 2];

                System.arraycopy(chunk, 0, chunkM, 0, chunkM.length);
Debug.println(Level.FINE, "wave l chunk(" + i + "): " + chunkM.length);

                // adpcm data L
                events.add(getVoiceEvent(streamNumber++, 1, sampleRate, chunkM)); // TODO channel is 1

                System.arraycopy(chunk, chunkM.length, chunkM, 0, chunkM.length);
Debug.println(Level.FINE, "wave r chunk(" + i + "): " + chunkM.length);

                // adpcm data R
                events.add(getVoiceEvent(streamNumber++, 1, sampleRate, chunkM)); // TODO channel is 1
            }
        }
        if (moduloOfChunks != 0) {
            byte[] temp = new byte[moduloOfChunks];
            System.arraycopy(pcm, (PCM_MAX_BLOCK * channels) * numberOfChunks, temp, 0, moduloOfChunks);
            byte[] chunk = audioEngine.encode(4, channels, temp);
            if (channels == 1) {
Debug.println(Level.FINE, "wave chunk(" + numberOfChunks + "): " + chunk.length);

                // adpcm data
                events.add(getVoiceEvent(streamNumber++, channels, sampleRate, chunk));

            } else {
                byte[] chunkM = new byte[chunk.length / 2];

                System.arraycopy(chunk, 0, chunkM, 0, chunkM.length);
Debug.println(Level.FINE, "wave l chunk(" + numberOfChunks + "): " + chunkM.length);

                // adpcm data L
                events.add(getVoiceEvent(streamNumber++, 1, sampleRate, chunkM)); // TODO channel is 1

                System.arraycopy(chunk, chunkM.length, chunkM, 0, chunkM.length);
Debug.println(Level.FINE, "wave r chunk(" + numberOfChunks + "): " + chunkM.length);

                // adpcm data R
                events.add(getVoiceEvent(streamNumber++, 1, sampleRate, chunkM)); // TODO channel is 1
            }
        }

        // 2. stream pan
        if (channels != 1) {
            streamNumber = 0;
            for (int i = 0; i < numberOfChunks; i++) {
                // adpcm pan L
                events.add(NecMessage.getPanEvent(L, streamNumber++, 0x00));
                // adpcm pan R
                events.add(NecMessage.getPanEvent(R, streamNumber++, 0x3f));
            }
            if (moduloOfChunks != 0) {
                // adpcm pan L
                events.add(NecMessage.getPanEvent(L, streamNumber++, 0x00));
                // adpcm pan R
                events.add(NecMessage.getPanEvent(R, streamNumber++, 0x3f));
            }
        }

        // 3. stream on
        initSequence();
        int blockDelta = (int) ((float) delta * MAX_BLOCK / (pcm.length / 4 / channels));
        streamNumber = 0;
        for (int i = 0; i < numberOfChunks; i++) {
            if (channels == 1) {
                // adpcm on
                events.add(getStreamOn(streamNumber++, velocity));
            } else {
                // adpcm on L
                events.add(NecMessage.getStreamOnEvent(L, streamNumber++, velocity));
                // adpcm on R
                events.add(NecMessage.getStreamOnEvent(R, streamNumber++, velocity));
            }
            // nop2
            for (int j = 0; j < blockDelta / Nop2Message.maxDelta; j++) {
                Nop2Message nop2 = new Nop2Message(0xff, 0xff);
                events.add(new MfiEvent(nop2, 0L));
            }
            int moduloOfBlockDelta = blockDelta % Nop2Message.maxDelta;
            Nop2Message nop2 = new Nop2Message(moduloOfBlockDelta % 0x100, moduloOfBlockDelta / 0x100);
            events.add(new MfiEvent(nop2, 0L));
        }
        if (moduloOfChunks != 0) {
            if (channels == 1) {
                // adpcm on
                events.add(getStreamOn(streamNumber++, velocity));
            } else {
                // adpcm on L
                events.add(NecMessage.getStreamOnEvent(L, streamNumber++, velocity));
                // adpcm on R
                events.add(NecMessage.getStreamOnEvent(R, streamNumber++, velocity));
            }
            // nop2
            int moduloOfDelta = delta % blockDelta;
            for (int j = 0; j < moduloOfDelta / Nop2Message.maxDelta; j++) {
                Nop2Message nop2 = new Nop2Message(0xff, 0xff);
                events.add(new MfiEvent(nop2, 0L));
            }
            int moduloOfBlockDelta = moduloOfDelta % Nop2Message.maxDelta;
            Nop2Message nop2 = new Nop2Message(moduloOfBlockDelta % 0x100, moduloOfBlockDelta / 0x100);
            events.add(new MfiEvent(nop2, 0L));
        }

        return events;
    }

    /** 0x01 0xf0 0x_7 */
    private static MfiEvent getVoiceEvent(int streamNumber, int channels, int sampleRate, byte[] adpcm) throws InvalidMfiDataException {
        MachineDependentMessage message = new NecMessage();
        Function1_240_7 function = new Function1_240_7();
        function.setStreamNumber(streamNumber);
        function.setMono(channels == 1);
        function.setFormat(1);
        function.setSamplingRate(sampleRate);
        function.setAdpcm(adpcm);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    /**
     * Creates max gain message.
     * <pre>
     * 0x01 0xf3 0x_3
     * </pre>
     * @param maxGain (wav2mld use 0x00)
     */
    public static MfiEvent getMaxGainEvent(int maxGain) throws InvalidMfiDataException {
        MachineDependentMessage message = new NecMessage();
        Function1_243_3 function = new Function1_243_3();
        function.setMaxGain(maxGain);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    /**
     * Creates stream number setting message.
     * <pre>
     * 0x01 0xf3 0x_4
     * </pre>
     * @param maxStreamNumber (wav2mld use 0x02)
     */
    public static MfiEvent getSettingEvent(int maxStreamNumber) throws InvalidMfiDataException {
        MachineDependentMessage message = new NecMessage();
        Function1_243_4 function = new Function1_243_4();
        function.setMaxStreamNumber(maxStreamNumber);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    /**
     * Creates adpcm on message.
     * <pre>
     * 0x01 0xf1 0x_3
     * </pre>
     */
    public static MfiEvent getStreamOnEvent(int channel, int streamNumber, int velocity) throws InvalidMfiDataException {
        MachineDependentMessage message = new NecMessage();
        Function1_241_3 function = new Function1_241_3();
        function.setChannel(channel);
        function.setStreamNumber(streamNumber);
        function.setVelocity(velocity);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    /**
     * Creates adpcm slave on message.
     * <pre>
     * 0x01 0xf1 0x_4
     * </pre>
     */
    public static MfiEvent getStreamSlaveOnEvent(int channel, int streamNumber, int velocity) throws InvalidMfiDataException {
        MachineDependentMessage message = new NecMessage();
        Function1_241_4 function = new Function1_241_4();
        function.setChannel(channel);
        function.setStreamNumber(streamNumber);
        function.setVelocity(velocity);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    /**
     * Creates adpcm volume message.
     * <pre>
     * 0x01 0xf2 0x_1
     * </pre>
     * <li> maybe deprecated since MFi 3.0
     */
    public static MfiEvent getVolumeEvent(int volume) throws InvalidMfiDataException {
        MachineDependentMessage message = new NecMessage();
        Function242_1 function = new Function242_1();
        function.setVolume(volume);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    /**
     * Creates stream pan message.
     * <pre>
     * 0x01 0xf1 0x_6
     * </pre>
     */
    public static MfiEvent getPanEvent(int channel, int streamNumber, int pan) throws InvalidMfiDataException {
        MachineDependentMessage message = new NecMessage();
        Function1_241_6 function = new Function1_241_6();
        function.setChannel(channel);
        function.setStreamNumber(streamNumber);
        function.setPan(pan);
        message.setMessage(0x00, function.getMessage());
        return new MfiEvent(message, 0L);
    }

    //----

    /**
     * Creates adpcm data message. (original pure stereo, TODO cannot play...)
     * <pre>
     * 0x01 0xf0 0x_7
     * </pre>
     * @param pcm wave (PCM) data (4bit required)
     * @param sampleRate 4000 ~ 16000 are available
     * @param bits bits ignored, assumed 4
     */
    public static List<MfiEvent> getAdpcmEventsEx(byte[] pcm, float time, int sampleRate, int bits, int channels, int adpcmVolume) throws InvalidMfiDataException {
        int delta = getDelta(time);
Debug.println(Level.FINE, "delta: " + delta + ", time: " + time);
        int velocity = (int) (adpcmVolume * maxVelocity / 100f);

        AudioEngine audioEngine = NecSequencer.getAudioEngine();

        byte[] adpcm = audioEngine.encode(4, channels, pcm);
Debug.println(Level.FINE, "adpcm length: " + adpcm.length);
//System.err.println("pcm:\n" + StringUtil.getDump(pcm, 64) + "adpcm L:\n" + StringUtil.getDump(adpcm, 64) +"adpcm R:\n" + StringUtil.getDump(adpcm, adpcm.length / 2, 64));

        int numberOfChunks = adpcm.length / MAX_BLOCK;
        int moduloOfChunks = adpcm.length % MAX_BLOCK;

        List<MfiEvent> events = new ArrayList<>();

        // 1. data
        int streamNumber = 0;
        for (int i = 0; i < numberOfChunks; i++) {
            byte[] chunk = new byte[MAX_BLOCK];
            if (channels == 1) {
                System.arraycopy(adpcm, MAX_BLOCK * i, chunk, 0, MAX_BLOCK);
            } else {
                System.arraycopy(adpcm, (MAX_BLOCK / 2) * i, chunk, 0, MAX_BLOCK / 2);
                System.arraycopy(adpcm, (adpcm.length / 2) + (MAX_BLOCK / 2) * i, chunk, MAX_BLOCK / 2, MAX_BLOCK / 2);
            }
Debug.println(Level.FINE, "wave chunk(" + i + "): " + chunk.length);

            // adpcm data
            events.add(getVoiceEvent(streamNumber++, channels, sampleRate, chunk));
        }
        if (moduloOfChunks != 0) {
            byte[] chunk = new byte[moduloOfChunks];
            if (channels == 1) {
                System.arraycopy(adpcm, MAX_BLOCK * numberOfChunks, chunk, 0, moduloOfChunks);
            } else {
                System.arraycopy(adpcm, (MAX_BLOCK / 2) * numberOfChunks, chunk, 0, moduloOfChunks / 2);
                System.arraycopy(adpcm, (adpcm.length / 2) + (MAX_BLOCK / 2) * numberOfChunks, chunk, moduloOfChunks / 2, moduloOfChunks / 2);
            }
Debug.println(Level.FINE, "wave chunk(" + numberOfChunks + "): " + chunk.length);

            // adpcm data
            events.add(getVoiceEvent(streamNumber++, channels, sampleRate, chunk));
        }

        // 2. stream on
        initSequence();
        int blockDelta = (int) ((float) delta * MAX_BLOCK / (adpcm.length * channels));
        streamNumber = 0;
        for (int i = 0; i < numberOfChunks; i++) {
            // adpcm on
Debug.println(Level.FINER, "thread local sequence: " + sequence.get());
            events.add(getStreamOn(streamNumber++, velocity));
            // nop2
            for (int j = 0; j < blockDelta / Nop2Message.maxDelta; j++) {
                Nop2Message nop2 = new Nop2Message(0xff, 0xff);
                events.add(new MfiEvent(nop2, 0L));
            }
            int moduloOfDelta = blockDelta % Nop2Message.maxDelta;
            Nop2Message nop2 = new Nop2Message(moduloOfDelta % 0x100, moduloOfDelta / 0x100);
            events.add(new MfiEvent(nop2, 0L));
        }
        if (moduloOfChunks != 0) {
            // adpcm on
Debug.println(Level.FINER, "thread local sequence: " + sequence.get());
            events.add(getStreamOn(streamNumber++, velocity));
            // nop2
            int moduloOfBlockDelta = delta % blockDelta;
            for (int j = 0; j < moduloOfBlockDelta / Nop2Message.maxDelta; j++) {
                Nop2Message nop2 = new Nop2Message(0xff, 0xff);
                events.add(new MfiEvent(nop2, 0L));
            }
            int moduloOfDelta = moduloOfBlockDelta % Nop2Message.maxDelta;
            Nop2Message nop2 = new Nop2Message(moduloOfDelta % 0x100, moduloOfDelta / 0x100);
            events.add(new MfiEvent(nop2, 0L));
        }

        return events;
    }
}
