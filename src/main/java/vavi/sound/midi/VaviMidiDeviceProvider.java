/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;


import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;

import vavi.util.Debug;


/**
 * VaviMidiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class VaviMidiDeviceProvider extends MidiDeviceProvider {

    /** 勝手に使用 */
    public final static int MANUFACTURER_ID = 0x5f;

    /** */
    private static final MidiDevice.Info[] infos = new MidiDevice.Info[] { VaviSequencer.info };

    /* */
    public MidiDevice.Info[] getDeviceInfo() {
        return infos;
    }

    /** ADPCM 再生機構を付加した MIDI シーケンサを返します。 */
    public MidiDevice getDevice(MidiDevice.Info info)
        throws IllegalArgumentException {

Debug.println("★1 info: " + info);
        if (info == VaviSequencer.info) {
            VaviSequencer wrappedSequencer = new VaviSequencer();
            return wrappedSequencer;
        } else {
Debug.println("★1 here");
            throw new IllegalArgumentException();
        }
    }
}

/* */
