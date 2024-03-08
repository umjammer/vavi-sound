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
    implements ChannelMessage, MidiConvertible, MfiConvertible {

    /** */
    private int voice;
    /** 0 - 63 */
    private int pitchBend;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
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
    public PitchBendMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xe4, data2);

        this.voice = (data2 & 0xc0) >> 6;
        this.pitchBend = data2 & 0x3f;
    }

    /** for {@link MfiConvertible} */
    public PitchBendMessage() {
        super(0, 0xff, 0xe4, 0);
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

    public String toString() {
        return "PitchBend:" + " voice=" + voice + " pitchBend=" + pitchBend;
    }

    // ----

    /**
     * because PsmPlayer converted it like this.
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int channel = getVoice() + 4 * context.getMfiTrackNumber();
//Debug.println(this);
//      context.setPitchBend(channel, getPitchBend());

//      int pitch = context.retrieveRealPitch(channel);

        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.PITCH_BEND,
                                channel,
                                0,                // LSB
                                getPitchBend() * 2);    // MSB
        return new MidiEvent[] {
            new MidiEvent(shortMessage, context.getCurrent())
        };
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
