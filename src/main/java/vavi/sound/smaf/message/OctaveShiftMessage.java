/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import javax.sound.midi.MidiEvent;


/**
 * OctaveShiftMessage.
 * <p>
 * HandyPhoneStandard only.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class OctaveShiftMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible {

    /** smaf channel 0 ~ 3 */
    private int channel;

    /**
     * <pre>
     * 0x00 No Shift(Original)
     * 0x01 +1 Octave
     * 0x02 +2 Octave
     * 0x03 +3 Octave
     * 0x04 +4 Octave
     * 0x05 ~ 0x80 Reserved
     * 0x81 -1 Octave
     * 0x82 -2 Octave
     * 0x83 -3 Octave
     * 0x84 -4 Octave
     * 0x85 ~ 0xff Reserved
     * </pre>
     */
    private int octaveShift;

    /**
     * Creates OctaveShiftMessage.
     * @param duration
     * @param channel smaf channel 0 ~ 3
     * @param value octaveShift 0x00 ~ 0x04, 0x81 ~ 0x84
     */
    public OctaveShiftMessage(int duration, int channel, int value) {
        this.duration = duration;
        this.channel = channel;
        this.octaveShift = value;
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
        return "OctaveShift:" +
            " duration=" + duration +
            " channel=" + channel +
            " octaveShift=" + octaveShift;
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

    /** Modify context. */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) {
        context.setOctaveShift(channel, octaveShift);

        return null;
    }
}
