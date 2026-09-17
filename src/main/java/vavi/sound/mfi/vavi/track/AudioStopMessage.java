/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import vavi.sound.mfi.ChannelMessage;
import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.LongMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.MobileExclusive;

import static java.lang.System.getLogger;
import static vavi.sound.mobile.MobileExclusive.off;
import static vavi.sound.mobile.MobileExclusive.packedSystex;


/**
 * AudioStopMessage.
 * <pre>
 *  0x7f, 0x01
 *  channel true
 *  delta   ?
 * </pre>
 * system property
 * <li>{@code vavi.sound.mobile.AudioEngine.disabled} ... not to use vavi.sound.mobile.AudioEngine but
 * to send {@link MobileExclusive#off} to the synthesizer, default {@code false}</li>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070117 nsano initial version <br>
 */
public class AudioStopMessage extends LongMessage
    implements ChannelMessage, MidiConvertible, AudioDataSequencer, TrackMessage {

    private static final Logger logger = getLogger(AudioStopMessage.class.getName());

    /** */
    private int voice;
    /** */
    private int index;

    @Override
    public boolean accept(String key) {
        return "127.a.1".equals(key);
    }

    /**
     * for {@link TrackChunk}
     * @param delta delta time
     * @param status must be 0x7f
     * @param data1 must be 0x01
     * @param data2 1 byte
     */
    @Override
    public AudioStopMessage init(int delta, int status, int data1, byte[] data2) {
        super.init(delta, 0x7f, 0x01, data2);

        this.voice = (data2[0] & 0xc0) >> 6;
        this.index =  data2[0] & 0x3f;

        return this;
    }

    @Override
    public int getVoice() {
        return voice;
    }

    /** */
    public int getIndex() {
        return index;
    }

    @Override
    public void setVoice(int voice) {
        this.voice = voice & 0x03;
        this.data[3] = (byte) ((this.data[3] & 0x3f) | (this.voice << 6));
    }

    @Override
    public String toString() {
        return "AudioStop:" +
        " voice=" + voice +
        " index=" + index;
    }

    // ----

    /**
     * @throws InvalidMidiDataException
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {

        SysexMessage sysexMessage;

        sysexMessage = packedSystex(off(MFi_SYSEX_FUNCTION_ID_MFi4, index));

        return new MidiEvent[] {
            new MidiEvent(sysexMessage, context.getCurrent())
        };
    }

    /**
     * @param data 12 id
     * @throws IllegalArgumentException when audio engine does not found
     * @see MobileExclusive#off
     */
    @Override
    public void sequence(byte[] data, Receiver receiver) throws InvalidMfiDataException {
        assert data[0] == 0x12 : "illegal command";
        int id = data[1] & 0x7f;

        AudioEngine engine = AudioEngineFactory.getAudioEngine();
        if (engine != null)
            AudioEngine.Sync.schedule(() -> engine.stop(id));
        else
            logger.log(Level.ERROR, "audio engine is not set");
    }
}
