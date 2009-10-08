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
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020629 nsano initial version <br>
 */
public abstract class MfiDeviceProvider {

    /** */
    public boolean isDeviceSupported(MfiDevice.Info info) {
        return false;
    }

    /** */
    public abstract MfiDevice.Info[] getDeviceInfo();

    /** */
    public abstract MfiDevice getDevice(MfiDevice.Info info);
}

/* */
