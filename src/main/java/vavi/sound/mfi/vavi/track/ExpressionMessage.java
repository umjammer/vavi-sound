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
 * ExpressionMessage.
 * <pre>
 *  0xff, 0xe# 音源制御情報
 *  channel true
 *  delta   true
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 030821 nsano implements {@link MidiConvertible} <br>
 *          0.02 030906 nsano implements {@link MfiConvertible} <br>
 *          0.03 030920 nsano repackage <br>
 *          0.04 031127 nsano fix voice <br>
 *          0.05 031203 nsano implements {@link ChannelMessage} <br>
 * @since MFi2
 */
public class ExpressionMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, MfiConvertible {

    /** */
    private int voice;
    /** -32 ~ 0 ~ 31 */
    private int volume;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 0xe6
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- volume
     *  +- voice
     * </pre>
     */
    public ExpressionMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xe6, data2);

        this.voice   = (data2 & 0xc0) >> 6;
        this.volume  =  data2 & 0x1f;
        if ((data2 & 0x20) != 0) {
            volume -= 64;
        }
    }

    /** for {@link MfiConvertible} */
    public ExpressionMessage() {
        super(0, 0xff, 0xe6, 0);
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

    /** */
    public String toString() {
        return "Expression:" +
            " voice="  + voice +
            " volume=" + volume;
    }

    //----

    /** TODO エクスプレッションとみなしたけどいいの？ */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int channel = getVoice() + 4 * context.getMfiTrackNumber();

//Debug.println("volume rel: " + channel + ": " + getVolume());
//      context.addVolume(channel, getVolume());

//      MidiEvent[] events = new MidiEvent[2];
//      ShortMessage shortMessage = new ShortMessage();
//      shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
//                  channel,
//                  7,    // メインボリューム MSB
//                  context.getVolume(channel) * 2);
//      events[0] = new MidiEvent(shortMessage, context.getCurrent());
//      shortMessage = new ShortMessage();
//      shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
//                  channel,
//                  39,    // メインボリューム LSB
//                  0);
//      events[1] = new MidiEvent(shortMessage, context.getCurrent());
        MidiEvent[] events = new MidiEvent[1];
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                channel,
                                11,    // エクスプレッション MSB
                                getVolume() < 0 ? getVolume() * 2 + 128 :
                                                  getVolume() * 2);
        events[0] = new MidiEvent(shortMessage, context.getCurrent());
        return events;
    }

    @Override
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();

        int track = context.retrieveMfiTrack(channel);
        int voice = context.retrieveVoice(channel);

        ExpressionMessage mfiMessage = new ExpressionMessage();
        mfiMessage.setDelta(context.getDelta(context.retrieveMfiTrack(channel)));
        mfiMessage.setVoice(voice);
        mfiMessage.setVolume(shortMessage.getData2() / 2);

        context.setPreviousTick(track, midiEvent.getTick());

        return new MfiEvent[] {
            new MfiEvent(mfiMessage, midiEvent.getTick())
        };
    }
}

/* */
