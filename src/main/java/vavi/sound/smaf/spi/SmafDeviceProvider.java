/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.spi;

import vavi.sound.smaf.SmafDevice;


/**
 * SmafDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260912 nsano initial version <br>
 * @see javax.sound.midi.spi.MidiDeviceProvider
 */
public abstract class SmafDeviceProvider {

    /** @see javax.sound.midi.spi.MidiDeviceProvider#isDeviceSupported(javax.sound.midi.MidiDevice.Info) */
    public boolean isDeviceSupported(SmafDevice.Info info) {
        return false;
    }

    /** @see javax.sound.midi.spi.MidiDeviceProvider#getDeviceInfo() */
    public abstract SmafDevice.Info[] getDeviceInfo();

    /** @see javax.sound.midi.spi.MidiDeviceProvider#getDevice(javax.sound.midi.MidiDevice.Info) */
    public abstract SmafDevice getDevice(SmafDevice.Info info);
}
