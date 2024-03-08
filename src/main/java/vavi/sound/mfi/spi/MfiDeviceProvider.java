/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.spi;

import vavi.sound.mfi.MfiDevice;


/**
 * MfiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020629 nsano initial version <br>
 * @see javax.sound.midi.spi.MidiDeviceProvider
 */
public abstract class MfiDeviceProvider {

    /** @see javax.sound.midi.spi.MidiDeviceProvider#isDeviceSupported(javax.sound.midi.MidiDevice.Info) */
    public boolean isDeviceSupported(MfiDevice.Info info) {
        return false;
    }

    /** @see javax.sound.midi.spi.MidiDeviceProvider#getDeviceInfo() */
    public abstract MfiDevice.Info[] getDeviceInfo();

    /** @see javax.sound.midi.spi.MidiDeviceProvider#getDevice(javax.sound.midi.MidiDevice.Info) */
    public abstract MfiDevice getDevice(MfiDevice.Info info);
}
