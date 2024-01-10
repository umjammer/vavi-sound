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
 * PanpotMessage.
 * <pre>
 *  0xff, 0xe# 音源制御情報
 *  channel true
 *  delta   true
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 030821 nsano implements {@link MidiConvertible} <br>
 *          0.02 030826 nsano complete <br>
 *          0.03 030906 nsano implements {@link MfiConvertible} <br>
 *          0.04 030920 nsano repackage <br>
 *          0.05 031203 nsano implements {@link ChannelMessage} <br>
 * @since MFi2
 */
public class PanpotMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, MfiConvertible {

    /** */
    private int voice;
    /** left 0, 1 - center 32 - right 63 */
    private int panpot = 32;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 0xe3
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- panpot
     *  +- voice
     * </pre>
     */
    public PanpotMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xe3, data2);

        this.voice  = (data2 & 0xc0) >> 6;
        this.panpot =  data2 & 0x3f;
    }

    /** for {@link MfiConvertible} */
    public PanpotMessage() {
        super(0, 0xff, 0xe3, 0);
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

    /** */
    public String toString() {
        return "Panpot:" +
            " voice="  + voice +
            " panpot=" + panpot;
    }

    //----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int channel = getVoice() + 4 * context.getMfiTrackNumber();

        MidiEvent[] events = new MidiEvent[1];
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                channel,
                                10,    // パンポット MSB
                                getPanpot() * 2);
        events[0] = new MidiEvent(shortMessage, context.getCurrent());
//      shortMessage = new ShortMessage();
//      shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
//                              channel,
//                              42,    // パンポット LSB
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

        PanpotMessage mfiMessage = new PanpotMessage();
        mfiMessage.setDelta(context.getDelta(context.retrieveMfiTrack(channel)));
        mfiMessage.setVoice(voice);
        mfiMessage.setPanpot(data2 / 2);

        context.setPreviousTick(track, midiEvent.getTick());

        return new MfiEvent[] {
            new MfiEvent(mfiMessage, midiEvent.getTick())
        };
    }
}

/* */
