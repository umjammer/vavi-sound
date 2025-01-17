/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
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
 * FineTuneMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 241216 nsano port from MFi <br>
 */
public class FineTuneMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible, SmafConvertible {

    /** smaf channel */
    private int channel;

    /** 0 ~ 127 */
    private int fineTune;

    /**
     *
     * @param duration
     * @param channel smaf channel
     * @param value fine 0 ~ 127
     */
    public FineTuneMessage(int duration, int channel, int value) {
        this.duration = duration;
        this.channel = channel;
        this.fineTune = value & 0x7f;
    }

    /** for SmafConvertible */
    protected FineTuneMessage() {
    }

    /** */
    public int getFineTune() {
        return fineTune;
    }

    /** */
    public void setFineTune(int volume) {
        this.fineTune = volume & 0x7f;
    }

    /** */
    public int getChannel() {
        return channel;
    }

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    @Override
    public String toString() {
        return "Expression:" +
            " duration=" + duration +
            " channel=" + channel +
            " fineTune="  + fineTune;
    }

    // ----

    @Override
    public byte[] getMessage() {
        return null; // TODO
    }

    @Override
    public int getLength() {
        return 0;   // TODO
    }

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        MidiEvent[] events = new MidiEvent[1];
        ShortMessage shortMessage = new ShortMessage();
//logger.log(Level.TRACE, "FineTune: [" + duration + "] " + channel + "ch, " + fineTune);
        shortMessage.setMessage(ShortMessage.TUNE_REQUEST, // TODO
                fineTune & 0x7f,   // LSB
                fineTune >> 7      // MSB
        );
        events[0] = new MidiEvent(shortMessage, context.getCurrentTick());
        return events;
    }

    @Override
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context)
        throws InvalidSmafDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();

        int track = context.retrieveSmafTrack(channel);
        int voice = context.retrieveVoice(channel);

        FineTuneMessage smafMessage = new FineTuneMessage();
        smafMessage.setDuration(context.getDuration());
        smafMessage.setChannel(voice);
        smafMessage.setFineTune(shortMessage.getData2()); // TODO data is 16bit

        context.setBeforeTick(track, midiEvent.getTick());

        return new SmafEvent[] {
            new SmafEvent(smafMessage, midiEvent.getTick())
        };
    }
}
