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
 * ModulationMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class ModulationMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible, SmafConvertible {

    /** smaf channel */
    private int channel;

    /** 0x00 ~ 0x7f */
    private int modulation;

    /**
     * Creates ModulationMessage.
     * @param duration delta time
     * @param channel smaf channel
     * @param value modulation 0x00 ~ 0x7f
     */
    public ModulationMessage(int duration, int channel, int value) {
        this.duration = duration;
        this.channel = channel;
        this.modulation = value;
    }

    /** for SmafConvertible */
    protected ModulationMessage() {
    }

    /** */
    public int getModulation() {
        return modulation;
    }

    /** */
    public void setModulation(int modulation) {
        this.modulation = modulation & 0x7f;
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
        return "Modulation:" +
            " duration=" + duration +
            " channel=" + channel +
            " modulation=" + modulation;
    }

    // ----

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
                                1,    // modulation depth MSB
                                modulation);
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

        ModulationMessage smafMessage = new ModulationMessage();
        smafMessage.setDuration(context.getDuration());
        smafMessage.setChannel(voice);
        smafMessage.setModulation(data2);

        context.setBeforeTick(track, midiEvent.getTick());

        return new SmafEvent[] {
            new SmafEvent(smafMessage, midiEvent.getTick())
        };
    }
}
