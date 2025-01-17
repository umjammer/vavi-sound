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
 * VolumeMessage.
 * <pre>
 *  0xe# Sound Source Control Information
 *  channel true
 *  delta   true
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 020627 nsano refine <br>
 *          0.11 030821 nsano implements {@link MidiConvertible} <br>
 *          0.12 030906 nsano implements {@link MfiConvertible} <br>
 *          0.13 030920 nsano repackage <br>
 *          0.14 031203 nsano implements {@link ChannelMessage} <br>
 */
public class VolumeMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, MfiConvertible {

    /** */
    private int voice;
    /** 0 - 63 */
    private int volume = 63;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 0xe2
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- volume
     *  +- voice
     * </pre>
     */
    public VolumeMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xe2, data2);

        this.voice  = (data2 & 0xc0) >> 6;
        this.volume =  data2 & 0x3f;
    }

    /** for {@link MfiConvertible} */
    public VolumeMessage() {
        super(0, 0xff, 0xe2, 0);
    }

    /** */
    public int getVolume() {
        return volume;
    }

    /** */
    public void setVolume(int volume) {
        this.volume = volume & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0xc0) | this.volume);
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
        return "Volume:" +
               " voice="  + voice +
               " volume=" + volume;
    }

    // ----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int channel = getVoice() + 4 * context.getMfiTrackNumber();

//logger.log(Level.TRACE, "volume: " + channel + ": " + getVolume());
        context.setVolume(channel, volume);

        MidiEvent[] events = new MidiEvent[1];
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                channel,
                                7,          // main volume MSB
                                context.getVolume(channel) * 2);
        events[0] = new MidiEvent(shortMessage, context.getCurrent());
//      shortMessage = new ShortMessage();
//      shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
//                              channel,
//                              39,         // main volume LSB
//                              0);
//      events[1] = new MidiEvent(shortMessage, context.getCurrent());
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

        VolumeMessage mfiMessage = new VolumeMessage();
        mfiMessage.setDelta(context.getDelta(context.retrieveMfiTrack(channel)));
        mfiMessage.setVoice(voice);
        mfiMessage.setVolume(data2 / 2);
//logger.log(Level.TRACE, mfiMessage.getVoice() + ", " + ((mfiMessage.getMessage()[3] & 0xc0) >> 6));

        context.setPreviousTick(track, midiEvent.getTick());

        return new MfiEvent[] {
            new MfiEvent(mfiMessage, midiEvent.getTick())
        };
    }
}
