/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;


/**
 * PanMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class PanMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible, SmafConvertible {

    /** smaf channel */
    private int channel;

    /** left 0, 1 - center 64 - right 127 */
    private int panpot;

    /**
     * Creates PanMessage.
     * @param duration
     * @param channel smaf channel
     * @param value left 0, 1 - center 64 - right 127
     */
    public PanMessage(int duration, int channel, int value) {
        this.duration = duration;
        this.channel = channel;
        this.panpot = value;
    }

    /** for SmafConvertible */
    protected PanMessage() {
    }

    /** */
    public int getPanpot() {
        return panpot;
    }

    /** */
    public void setPanpot(int panpot) {
        this.panpot = panpot & 0x7f;
    }

    /** */
    public int getChannel() {
        return channel;
    }

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public String toString() {
        return "Pan:" +
            " duration=" + duration +
            " channel=" + channel +
            " panpot=" + panpot;
    }

    //----

    /* */
    @Override
    public byte[] getMessage() {
        return null; // TODO
    }

    /* */
    @Override
    public int getLength() {
        return 0;   // TODO
    }

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int midiChannel = context.retrieveChannel(this.channel);

        MidiEvent[] events = new MidiEvent[1];
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                midiChannel,
                                10,    // パンポット MSB
                                panpot);
        events[0] = new MidiEvent(shortMessage, context.getCurrentTick());
        return events;
    }

    @Override
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context)
        throws InvalidSmafDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int data2 = shortMessage.getData2();

        int track = context.retrieveSmafTrack(channel);
        int voice = context.retrieveVoice(channel);

        PanMessage smafMessage = new PanMessage();
        smafMessage.setDuration(context.getDuration());
        smafMessage.setChannel(voice);
        smafMessage.setPanpot(data2);

        context.setBeforeTick(track, midiEvent.getTick());

        return new SmafEvent[] {
            new SmafEvent(smafMessage, midiEvent.getTick())
        };
    }
}

/* */
