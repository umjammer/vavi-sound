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
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage;
import vavi.sound.midi.VaviMidiDeviceProvider;


/**
 * MasterVolumeMessage
 * <pre>
 *  0xff, 0xb0
 * </pre>
 * <p>
 * System Property
 * <li>vavi.sound.mfi.ignoreMasterVolume ... ignore this setting when user want to set master volume by himself, default false</li>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 030821 nsano implements {@link MidiConvertible} <br>
 *          0.02 030904 nsano complete <br>
 *          0.03 030920 nsano repackage <br>
 */
public class MasterVolumeMessage extends ShortMessage
    implements MidiConvertible, TrackMessage {

    /**
     * sysex function id: the universal master volume following this one is the song's, not the listener's
     * <pre>
     * 0xf0 0x45 0x05 volume 0xf7
     * </pre>
     * for a synthesizer of an mfi sound source which has both of them, the others do not know
     * the function and let it go, the universal one works for them as before.
     */
    public static final int SYSEX_FUNCTION_ID_MASTER_VOLUME = 0x05;

    /** 0 ~ 127 */
    private int volume = 100;

    @Override
    public boolean accept(String key) {
        return "255.b.176".equals(key);
    }

    /**
     * for {@link TrackChunk}
     * @param delta delta time
     * @param status
     * @param data1 0xb0
     * @param data2 volume
     */
    @Override
    public MasterVolumeMessage init(int delta, int status, int data1, int data2) {
        super.init(delta, 0xff, 0xb0, data2);

        this.volume = data2;    // 0 ~ 127

        return this;
    }

    /** */
    public int getVolume() {
        return volume;
    }

    @Override
    public String toString() {
        return "MasterVolume:" +
            " volume="  + volume;
    }

    // ----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        if (Boolean.parseBoolean(System.getProperty("vavi.sound.mfi.ignoreMasterVolume", "false")))
            return null;

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

        byte[] mark = {
                (byte) 0xf0,
                VaviMidiDeviceProvider.MANUFACTURER_ID,
                SYSEX_FUNCTION_ID_MASTER_VOLUME,
                (byte) (volume & 0x7f),
                (byte) 0xf7
        };
        SysexMessage markMessage = new SysexMessage();
        markMessage.setMessage(mark, mark.length);

        return new MidiEvent[] {
            new MidiEvent(markMessage, context.getCurrent()),
            new MidiEvent(sysexMessage, context.getCurrent())
        };
    }
}
