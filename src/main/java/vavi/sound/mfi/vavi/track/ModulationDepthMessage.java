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
 * ModulationDepthMessage.
 * <pre>
 *  0xff, 0xe# 音源制御情報
 *  channel true
 *  delta   true
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030618 nsano initial version <br>
 *          0.01 030821 nsano implements {@link MidiConvertible} <br>
 *          0.02 030826 nsano complete <br>
 *          0.03 030906 nsano implements {@link MfiConvertible} <br>
 *          0.04 030920 nsano repackage <br>
 *          0.05 031203 nsano implements {@link ChannelMessage} <br>
 */
public class ModulationDepthMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, MfiConvertible {

    /** */
    private int voice;
    /** 0 ~ 63 */
    private int modulationDepth;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 0xea
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- modulationDepth
     *  +- voice
     * </pre>
     */
    public ModulationDepthMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xea, data2);

        this.voice           = (data2 & 0xc0) >> 6;
        this.modulationDepth =  data2 & 0x3f;
    }

    /** for {@link MfiConvertible} */
    public ModulationDepthMessage() {
        super(0, 0xff, 0xea, 0);
    }

    /** */
    public int getModulationDepth() {
        return modulationDepth;
    }

    /** */
    public void setModulationDepth(int modulationDepth) {
        this.modulationDepth = modulationDepth & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0xc0) | this.modulationDepth);
    }

    /** */
    public int getVoice() {
        return voice;
    }

    /** */
    public void setVoice(int voice) {
        this.voice = voice & 0x03;
        this.data[3] = (byte) ((this.data[3] & 0x3f) | (this.voice << 6));
    }

    /** */
    public String toString() {
        return "ModulationDepth:" +
            " voice="           + voice +
            " modulationDepth=" + modulationDepth;
    }

    //----

    /** */
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int channel = getVoice() + 4 * context.getMfiTrackNumber();

        MidiEvent[] events = new MidiEvent[1];
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                channel,
                                1,    // モジュレーション・デプス MSB
                                getModulationDepth() * 2);
        events[0] = new MidiEvent(shortMessage, context.getCurrent());
//      shortMessage = new ShortMessage();
//      shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
//                  channel,
//                  33,    // モジュレーション・デプス LSB
//                  0);
//      events[1] = new MidiEvent(shortMessage, context.getCurrent());
        return events;
    }

    /** */
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int data2 = shortMessage.getData2();

        int track = context.retrieveMfiTrack(channel);
        int voice = context.retrieveVoice(channel);

        ModulationDepthMessage mfiMessage = new ModulationDepthMessage();
        mfiMessage.setDelta(context.getDelta(context.retrieveMfiTrack(channel)));
        mfiMessage.setVoice(voice);
        mfiMessage.setModulationDepth(data2 / 2);

        context.setPreviousTick(track, midiEvent.getTick());

        return new MfiEvent[] {
            new MfiEvent(mfiMessage, midiEvent.getTick())
        };
    }
}

/* */
