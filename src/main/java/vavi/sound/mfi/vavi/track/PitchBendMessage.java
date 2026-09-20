/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.mfi.ChannelMessage;
import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.vavi.MfiContext;
import vavi.sound.mfi.vavi.MfiConvertible;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage;


/**
 * PitchBendMessage.
 * <pre>
 *  0xff, 0xe# Sound Source Control Information
 *  channel true
 *  delta   true
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020706 nsano initial version <br>
 *          0.01 030821 nsano implements {@link MidiConvertible} <br>
 *          0.02 030906 nsano implements {@link MfiConvertible} <br>
 *          0.03 030920 nsano repackage <br>
 *          0.04 031203 nsano implements {@link ChannelMessage} <br>
 */
public class PitchBendMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, MfiConvertible, TrackMessage {

    /** */
    private int voice;
    /** 0 - 63 */
    private int pitchBend;

    @Override
    public boolean accept(String key) {
        return "255.b.228".equals(key);
    }

    /**
     * for {@link TrackChunk}
     * @param delta delta time
     * @param status
     * @param data1 0xe4
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- pitchBend
     *  +- voice
     * </pre>
     */
    @Override
    public PitchBendMessage init(int delta, int status, int data1, int data2) {
        super.init(delta, 0xff, 0xe4, data2);

        this.voice = (data2 & 0xc0) >> 6;
        this.pitchBend = data2 & 0x3f;

        return this;
    }

    /** for {@link MfiConvertible} */
    public PitchBendMessage init() {
        return (PitchBendMessage) super.init(0, 0xff, 0xe4, 0);
    }

    /** */
    public int getPitchBend() {
        return pitchBend;
    }

    /** */
    public void setPitchBend(int pitchBend) {
        this.pitchBend = pitchBend & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0xc0) | this.pitchBend);
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
        return "PitchBend:" + " voice=" + voice + " pitchBend=" + pitchBend;
    }

    // ----

    /**
     * The coarse half commits the bend, as PsmPlayer converted it: with the fine half at
     * rest the midi value is {@code pitchBend * 0x100}, the very
     * {@code (0, pitchBend * 2)} LSB / MSB pair this used to send on its own.
     * <p>
     * {@link PitchBendFineMessage} (0xe9) and {@link PitchBendFineAMessage} (0xe8) leave
     * their half in the {@link MidiContext} for this, so a bend that moves finely than a
     * coarse step now reaches a plain midi synthesizer as well - see
     * {@link MidiContext#retrievePitchBend(int)}.
     * </p>
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int channel = getVoice() + 4 * context.getMfiTrackNumber();
        // on where the channel goes, a percussion one to the drum channel, a melody one away from it
        int midiChannel = context.retrieveChannel(channel);
//logger.log(Level.TRACE, this);
        context.setPitchBend(channel, getPitchBend());

        int pitch = context.retrievePitchBend(channel);

        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.PITCH_BEND,
                                midiChannel,
                                pitch & 0x7f,           // LSB
                                (pitch >> 7) & 0x7f);   // MSB
        return context.withOrigins(channel, new MidiEvent[] {
            new MidiEvent(shortMessage, context.getCurrent())
        });
    }

    @Override
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int data2 = shortMessage.getData2();

        int track = context.retrieveMfiTrack(channel);
        int voice = context.retrieveVoice(channel);

        PitchBendMessage mfiMessage = new PitchBendMessage();
        mfiMessage.setDelta(context.getDelta(context.retrieveMfiTrack(channel)));
        mfiMessage.setVoice(voice);
        mfiMessage.setPitchBend(data2 / 2);

        context.setPreviousTick(track, midiEvent.getTick());

        return new MfiEvent[] {
            new MfiEvent(mfiMessage, midiEvent.getTick())
        };
    }
}
