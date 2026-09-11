/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.ChannelMessage;
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
 *      ({@link vavi.sound.smaf.message.PitchBendMessage}) while the MFi 0xe4
 *      message only carries 6, so a second half has to exist.</li>
 *  <li>0xe9 is immediately followed by 0xe4 in 140762 of the 140914 messages in
 *      the ~4400 file corpus.</li>
 *  <li>Combining them as {@code (pitchBend << 6) | fine} makes bend sweeps come
 *      out monotonic - the median step between two consecutive updates of the
 *      same voice is 176 and only 12% of steps exceed 512. The other order gives
 *      449 and 48%, i.e. noise.</li>
 * </ul>
 * <p>
 * TODO more investigation before feeding it to the synthesizer. Both halves rest
 * at 32, so the pair is either a plain 12 bit value whose centre is 0x820 or two
 * independently centred coarse / fine offsets - the monotonicity test cannot
 * tell those apart. Until that is settled {@link #getMidiEvents(MidiContext)}
 * deliberately emits nothing, which is what happened before this class existed;
 * wiring it up means giving {@link PitchBendMessage} the MIDI pitch bend LSB it
 * currently hard codes to 0.
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

    /** TODO see the class comment, this deliberately emits nothing for now */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) {
logger.log(Level.DEBUG, this);
        return null;
    }
}
