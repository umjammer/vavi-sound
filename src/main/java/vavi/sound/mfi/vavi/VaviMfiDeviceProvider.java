/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Properties;

import vavi.sound.mfi.MfiDevice;
import vavi.sound.mfi.spi.MfiDeviceProvider;

import static java.lang.System.getLogger;


/**
 * {@link MfiDeviceProvider} implemented by vavi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020629 nsano initial version <br>
 *          0.10 020703 nsano complete <br>
 *          0.11 030819 nsano add {@link vavi.sound.mfi.MidiConverter} <br>
 */
public class VaviMfiDeviceProvider extends MfiDeviceProvider {

    private static final Logger logger = getLogger(VaviMfiDeviceProvider.class.getName());

    static {
        try {
            try (InputStream is = VaviMfiDeviceProvider.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound/pom.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("version", "undefined in pom.properties");
                } else {
                    version = System.getProperty("vavi.test.version", "undefined");
                }
            }
        } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }

    /** */
    public static final String version;

    @Override
    public boolean isDeviceSupported(MfiDevice.Info info) {
        for (MfiDevice.Info mfiDeviceInfo : getDeviceInfo()) {
            if (mfiDeviceInfo.equals(info)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MfiDevice.Info[] getDeviceInfo() {
        return new MfiDevice.Info[] {
                VaviSequencer.info,
                VaviSynthesizer.info,
                VaviMidiConverter.info
        };
    }

    @Override
    public MfiDevice getDevice(MfiDevice.Info info) {
        if (info == VaviSynthesizer.info) {
            VaviSynthesizer synthesizer = new VaviSynthesizer();
            return synthesizer;
        } else if (info == VaviSequencer.info) {
            VaviSequencer sequencer = new VaviSequencer();
            return sequencer;
        } else if (info == VaviMidiConverter.info) {
            VaviMidiConverter converter = new VaviMidiConverter();
            return converter;
        } else {
            throw new IllegalArgumentException("info is not suitable for this provider");
        }
    }
}
