/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.mfi.ChannelMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage;
import vavi.sound.mfi.vavi.sequencer.MfiValueExclusive;


/**
 * The "A" fine pitch bend, the one that stands on its own.
 * <pre>
 *  0xff, 0xe8
 * </pre>
 * <p>
 * 0xe8 and {@link PitchBendFineMessage} (0xe9) write the <b>same</b> 6 bit fine half
 * of the pitch bend {@link PitchBendMessage} (0xe4) carries the coarse half of. What
 * tells them apart is when the value lands, and that is what "A" and "B" name:
 * </p>
 * <ul>
 *  <li>0xe9 rides with the 0xe4 that completes it - it is on the very tick of the next
 *      0xe4 of the same voice in 136233 of the 136234 times it occurs in the ~4400
 *      file corpus at {@code ~/Public/np2/mfi}, the one exception aside.</li>
 *  <li>0xe8 does not - of its 12660 occurrences 10713 come strictly before the next
 *      0xe4 of their voice, 537 have no 0xe4 after them at all, and only 1410 share a
 *      tick with one. It moves the pitch by itself, between coarse updates.</li>
 * </ul>
 * <p>
 * How it was identified, since no MFi document was available and, unlike 0xe9, no
 * converter of {@code tmp/SCP-MA-N-210-j} so much as mentions 0xe8 (the MA-3, MA-5 and
 * MA-7 dlls hold no 0xe8 constant at all, while {@code CnvMA5MFi_N.dll} maps 0xe9 at
 * {@code 0x1001964d}):
 * </p>
 * <ul>
 *  <li>the byte is split the same way every per voice MFi byte is - 32 is far and away
 *      its most common value (4021 of 12660), per voice, which is the rest of a 0 ~ 63
 *      range, and 8402 of the 12660 are above 31, so the whole 6 bits are in use.</li>
 *  <li>54 files of the corpus write 0xe8 and never write 0xe9, and there 0xe8 sits in
 *      exactly the slot 0xe9 has elsewhere, one per 0xe4 (the count matches the 0xe4
 *      count outright in 22 of them).</li>
 *  <li>the same test that settled 0xe9 settles this one, and more sharply: combining
 *      it as {@code (pitchBend << 6) | fine} gives a median step of 64 between two
 *      consecutive updates of the same voice with 7.1% of steps above 512, while the
 *      other order gives 384 and 42.0%. For 0xe9 the same figures are 192 / 19.6%
 *      against 392 / 47.1%.</li>
 *  <li>reading both as writes of one fine register makes a vibrato come out as a clean
 *      triangle: in {@code 67_8981100010347092588F.MLD} voice 0 the two alternate
 *      through a 18 tick cycle and {@code (pitchBend << 5) + fine} then steps by
 *      exactly +-13 / +-14 all the way round, turning at the ends, with no jump where
 *      the messages change over.</li>
 * </ul>
 * <p>
 * It therefore goes to the synthesizer as the same
 * {@link MfiValueExclusive#PITCH_BEND_FINE} {@link PitchBendFineMessage} sends. Where
 * the two part company is the midi pitch bend: 0xe9 leaves that to the 0xe4 that always
 * follows it, and 0xe8 usually has no 0xe4 behind it, so it makes one itself out of the
 * coarse half {@link MidiContext} holds for the channel - otherwise a fine only move
 * would not be heard at all until the next coarse update.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 * @see PitchBendFineMessage
 */
public class PitchBendFineAMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, TrackMessage {

    /** */
    private int voice;
    /** 0 ~ 63, 32 is neutral */
    private int pitchBendFine;

    @Override
    public boolean accept(String key) {
        return "255.b.232".equals(key);
    }

    /**
     * for {@link TrackChunk}
     * @param delta delta time
     * @param status
     * @param data1 0xe8
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- pitchBendFine
     *  +- voice
     * </pre>
     */
    @Override
    public PitchBendFineAMessage init(int delta, int status, int data1, int data2) {
        super.init(delta, 0xff, 0xe8, data2);

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
        return "PitchBendFineA:" +
            " voice="         + voice +
            " pitchBendFine=" + pitchBendFine;
    }

    // ----

    /**
     * Unlike {@link PitchBendFineMessage} this one commits the bend itself, because it
     * usually has no {@link PitchBendMessage} behind it to do that - 10713 of its 12660
     * occurrences in the corpus come strictly before the next 0xe4 of their voice and 537
     * have none after them at all. So it leaves its half in the {@link MidiContext}, goes
     * out as {@link MfiValueExclusive#PITCH_BEND_FINE} for a synthesizer of the sound
     * source the way the "B" half does, and then makes the midi pitch bend the coarse
     * half of the channel and this fine half come to
     * ({@link MidiContext#retrievePitchBend(int)}).
     * <p>
     * On the 1410 occasions it does share a tick with a 0xe4, that 0xe4 comes after it
     * and sends the same bend again with its own coarse half, which is the one that
     * counts.
     * </p>
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
        int channel = getVoice() + 4 * context.getMfiTrackNumber();
        // on where the channel goes, a percussion one to the drum channel, a melody one away from it
        int midiChannel = context.retrieveChannel(channel);
        context.setFinePitchBend(channel, getPitchBendFine());

        int pitch = context.retrievePitchBend(channel);

        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.PITCH_BEND,
                                midiChannel,
                                pitch & 0x7f,           // LSB
                                (pitch >> 7) & 0x7f);   // MSB
        return context.withOrigins(channel, new MidiEvent[] {
            new MidiEvent(MfiValueExclusive.message(MfiValueExclusive.PITCH_BEND_FINE, midiChannel, getPitchBendFine()), context.getCurrent()),
            new MidiEvent(shortMessage, context.getCurrent())
        });
    }
}
