/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import vavi.sound.mfi.ChannelMessage;
import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mobile.MobileExclusive;

import static vavi.sound.mobile.MobileExclusive.packedSysex;
import static vavi.sound.mobile.MobileExclusive.panpot;


/**
 * AudioChannelPanpotMessage.
 * <pre>
 *  0x7f, 0x81
 *  channel true
 *  delta   ?
 * </pre>
 * system property
 * <li>{@code vavi.sound.mobile.AudioEngine.disabled} ... not to use vavi.sound.mobile.AudioEngine but
 * to send {@link MobileExclusive#panpot} to the synthesizer, default {@code false}</li>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070117 nsano initial version <br>
 * @since MFi4
 */
public class AudioChannelPanpotMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, AudioDataSequencer, TrackMessage {

    /** */
    private int voice;
    /** left 0, 1 - center 32 - right 63 */
    private int panpot = 32;

    @Override
    public boolean accept(String key) {
        return "127.b.129".equals(key);
    }

    /**
     * for {@link TrackChunk}
     * @param delta delta time
     * @param status must be 0x7f
     * @param data1 must be 0x81
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- panpot
     *  +- voice
     * </pre>
     */
    @Override
    public AudioChannelPanpotMessage init(int delta, int status, int data1, int data2) {
        super.init(delta, 0x7f, 0x81, data2);

        this.voice  = (data2 & 0xc0) >> 6;
        this.panpot =  data2 & 0x3f;

        return this;
    }

    /** */
    public int getPanpot() {
        return panpot;
    }

    /** */
    public void setPanpot(int panpot) {
        this.panpot = panpot & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0xc0) | this.panpot);
    }

    @Override
    public int getVoice() {
        return voice;
    }

    @Override
    public void setVoice(int voice) {
        this.voice = voice & 0x03;
        this.data[3] = (byte) ((this.data[3] & 0x3f) | (this.voice << 6));
    }

    @Override
    public String toString() {
        return "AudioChannelPanpot:" +
            " voice="  + voice +
            " panpot=" + panpot;
    }

    // ----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        // not the yamaha stream panpot (43 79 06 7f 0b), which is of a wave, this is of a channel
        SysexMessage sysexMessage = packedSysex(panpot(MFi_SYSEX_FUNCTION_ID_MFi4, voice, panpot * 2));

        return new MidiEvent[] {
            new MidiEvent(sysexMessage, context.getCurrent())
        };
    }

    /**
     * @param data 14 ch pp
     * @throws IllegalArgumentException when audio engine does not found
     * @see MobileExclusive#panpot
     */
    @Override
    public void sequence(byte[] data, Receiver receiver) throws InvalidMfiDataException {
        assert data[0] == 0x14 : "illegal command";
        // TODO audio engine panpot
    }
}
