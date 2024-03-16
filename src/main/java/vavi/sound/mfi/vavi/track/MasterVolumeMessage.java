/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.SysexMessage;

import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;


/**
 * MasterVolumeMessage
 * <pre>
 *  0xff, 0xb0
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 030821 nsano implements {@link MidiConvertible} <br>
 *          0.02 030904 nsano complete <br>
 *          0.03 030920 nsano repackage <br>
 */
public class MasterVolumeMessage extends ShortMessage
    implements MidiConvertible {

    /** 0 ~ 127 */
    private int volume = 100;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 0xb0
     * @param data2 volume
     */
    public MasterVolumeMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xb0, data2);

        this.volume = data2;    // 0 ~ 127
    }

    /** */
    public int getVolume() {
        return volume;
    }

    /** */
    public String toString() {
        return "MasterVolume:" +
            " volume="  + volume;
    }

    //----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        byte[] data = new byte[8];

        data[0] = (byte) 0xf0;
        data[1] = (byte) 0x7f;      // ID number, 0x7e: no real time, 0x7f: real time
        data[2] = (byte) 0x7f;      // device ID, 0x7f not recognized as channel
        data[3] = (byte) 0x04;      // sub-ID#1, 0x04 Device Control
        data[4] = (byte) 0x01;      // sub-ID#2, 0x01 Master Volume
        data[5] = (byte) 0x00;      // data L
        data[6] = (byte) volume;    // data H
        data[7] = (byte) 0xf7;

        SysexMessage sysexMessage = new SysexMessage();
        sysexMessage.setMessage(data, data.length);
        return new MidiEvent[] {
            new MidiEvent(sysexMessage, context.getCurrent())
        };
    }
}
