/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.util.logging.Level;

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

    /**
     * TODO used without asking
     * TODO 0x5f is occupied by "SD Card Association"
     */
    public final static int MANUFACTURER_ID = 0x5f;

    /** */
    private static final MidiDevice.Info[] infos = new MidiDevice.Info[] { VaviSequencer.info };

    /* */
    public MidiDevice.Info[] getDeviceInfo() {
        return infos;
    }

    /**
     * ADPCM 再生機構を付加した MIDI シーケンサを返します。
     * @throws IllegalArgumentException info is not suitable for this provider
     */
    public MidiDevice getDevice(MidiDevice.Info info) {

        if (info == VaviSequencer.info) {
//new Exception("*** DUMMY ***").printStackTrace();
Debug.println(Level.FINE, "★1 info: " + info);
            VaviSequencer wrappedSequencer = new VaviSequencer();
            return wrappedSequencer;
        } else {
Debug.println(Level.FINE, "★1 not suitable for this provider: " + info);
            throw new IllegalArgumentException("info is not suitable for this provider");
        }
    }
}

/* */
