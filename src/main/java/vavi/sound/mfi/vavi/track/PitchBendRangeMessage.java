/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
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
 * PitchBendRangeMessage.
 * <pre>
 *  0xff, 0xe# Sound Source Control Information
 *  channel true
 *  delta   true
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030826 nsano initial version <br>
 *          0.01 030906 nsano implements {@kink MfiConvertible} <br>
 *          0.02 030920 nsano repackage <br>
 *          0.03 031203 nsano implements {@link ChannelMessage} <br>
 */
public class PitchBendRangeMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, MfiConvertible {

    /** */
    private int voice;
    /** 0 - 24, default is 2 */
    private int pitchBendRange;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 0xe7
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- pitchBendRange
     *  +- voice
     * </pre>
     */
    public PitchBendRangeMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xe7, data2);

        this.voice          = (data2 & 0xc0) >> 6;
        this.pitchBendRange =  data2 & 0x3f;
        if (pitchBendRange > 24) {
            throw new IllegalArgumentException("range is between 0 and 24");
        }
    }

    /** for {@link MfiConvertible} */
    public PitchBendRangeMessage() {
        super(0, 0xff, 0xe7, 0);
    }

    /** */
    public int getPitchBendRange() {
    return pitchBendRange;
    }

    /** TODO check 0 ~ 24 */
    public void setPitchBendRange(int pitchBendRange) {
        this.pitchBendRange = pitchBendRange & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0xc0) | this.pitchBendRange);
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
        return "PitchBendRange:" +
            " voice="          + voice +
            " pitchBendRange=" + pitchBendRange;
    }

    //----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int channel = getVoice() + 4 * context.getMfiTrackNumber();
//Debug.println(this);
//      context.setPitchBendRange(channel, getPitchBendRange());

        MidiEvent[] events = new MidiEvent[3];
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                channel,
                                100,        // RPN MSB
                                0);         // 0: pitch bend range
        events[0] = new MidiEvent(shortMessage, context.getCurrent());
        shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                channel,
                                101,        // RPN LSB
                                0);         // 0: pitch bend range
        events[1] = new MidiEvent(shortMessage, context.getCurrent());
        shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                channel,
                                6,          // Data Entry MSB
                                getPitchBendRange());
        events[2] = new MidiEvent(shortMessage, context.getCurrent());
        return events;
    }

    @Override
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int data2 = shortMessage.getData2();

        int track = context.retrieveMfiTrack(channel);
        int voice = context.retrieveVoice(channel);

        PitchBendRangeMessage mfiMessage = new PitchBendRangeMessage();
        mfiMessage.setDelta(context.getDelta(context.retrieveMfiTrack(channel)));
        mfiMessage.setVoice(voice);
//  if (data2 / 2 > 24) {
//   Debug.println("range: " + data2 / 2);
//  }
        mfiMessage.setPitchBendRange(Math.min(data2 / 2, 24));

        context.setPreviousTick(track, midiEvent.getTick());

        return new MfiEvent[] {
            new MfiEvent(mfiMessage, midiEvent.getTick())
        };
    }
}
