/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.lang.System.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.ChannelMessage;
import vavi.sound.mfi.vavi.sequencer.FuetrekMfiExclusive;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage;

import static java.lang.System.getLogger;


/**
 * The fine (low) half of a pitch bend, the companion of {@link PitchBendMessage}.
 * <pre>
 *  0xff, 0xe9
 * </pre>
 * <p>
 * MFi pitch bend is split over two messages: this one first, then
 * {@link PitchBendMessage} at the same delta on the same voice. Both carry a
 * 6 bit value and both rest at 32.
 * </p>
 * <p>
 * How it was identified, since no MFi document was available:
 * </p>
 * <ul>
 *  <li>{@code CnvMA5MFi_N.dll} (at {@code 0x1001964d}) maps the MFi byte 0xe9 to
 *      internal event 22, right between 0xe4 -&gt; 21 ({@link PitchBendMessage})
 *      and 0xe7 -&gt; 23 ({@link PitchBendRangeMessage}).</li>
 *  <li>SMAF's own pitch bend is 14 bit
 *      ({@link vavi.sound.smaf.vavi.message.PitchBendMessage}) while the MFi 0xe4
 *      message only carries 6, so a second half has to exist.</li>
 *  <li>0xe9 is immediately followed by 0xe4 in 140762 of the 140914 messages in
 *      the ~4400 file corpus.</li>
 *  <li>Combining them as {@code (pitchBend << 6) | fine} makes bend sweeps come
 *      out monotonic - the median step between two consecutive updates of the
 *      same voice is 176 and only 12% of steps exceed 512. The other order gives
 *      449 and 48%, i.e. noise.</li>
 * </ul>
 * <p>
 * The fuetrek native player (openDoJa's {@code FueTrekSampler}) settles how they are
 * combined: the pitch word is {@code (((pitchBend << 5) + fine) << 3) - 0x100}, 0x2000
 * when both rest at 32, this one is cached and {@link PitchBendMessage} commits.
 * Since the midi pitch bend {@link PitchBendMessage} makes is left as it is, this goes
 * as {@link FuetrekMfiExclusive#PITCH_BEND_FINE} for a synthesizer of the sound
 * source, the others let it go.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
public class PitchBendFineMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, TrackMessage {

    private static final Logger logger = getLogger(PitchBendFineMessage.class.getName());

    /** */
    private int voice;
    /** 0 ~ 63, 32 is neutral */
    private int pitchBendFine;

    @Override
    public boolean accept(String key) {
        return "255.b.233".equals(key);
    }

    /**
     * for {@link TrackChunk}
     * @param delta delta time
     * @param status
     * @param data1 0xe9
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- pitchBendFine
     *  +- voice
     * </pre>
     */
    @Override
    public PitchBendFineMessage init(int delta, int status, int data1, int data2) {
        super.init(delta, 0xff, 0xe9, data2);

        this.voice         = (data2 & 0xc0) >> 6;
        this.pitchBendFine =  data2 & 0x3f;

        return this;
    }

    /** 0 ~ 63, 32 is neutral */
    public int getPitchBendFine() {
        return pitchBendFine;
    }

    /** 0 ~ 63, 32 is neutral */
    public void setPitchBendFine(int pitchBendFine) {
        this.pitchBendFine = pitchBendFine & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0xc0) | this.pitchBendFine);
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
        return "PitchBendFine:" +
            " voice="         + voice +
            " pitchBendFine=" + pitchBendFine;
    }

    // ----

    /** the fine half as {@link FuetrekMfiExclusive#PITCH_BEND_FINE}, see the class comment */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
        int channel = getVoice() + 4 * context.getMfiTrackNumber();
        return new MidiEvent[] {
            new MidiEvent(FuetrekMfiExclusive.message(FuetrekMfiExclusive.PITCH_BEND_FINE, channel, getPitchBendFine()), context.getCurrent())
        };
    }
}
