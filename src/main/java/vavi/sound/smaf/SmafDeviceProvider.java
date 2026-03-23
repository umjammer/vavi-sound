/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.InputStream;
import java.lang.System.Logger;
import java.util.Properties;

import static java.lang.System.getLogger;


/**
 * {@link SmafDeviceProvider} implemented by vavi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071012 nsano initial version <br>
 */
public class SmafDeviceProvider {

    private static final Logger logger = getLogger(SmafDeviceProvider.class.getName());

    static {
        try {
            try (InputStream is = SmafDeviceProvider.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound/pom.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("version", "undefined in pom.properties");
                } else {
                    version = System.getProperty("vavi.test.version", "undefined");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    public static final String version;

    /**
     * TODO used without asking
     * 0x45 is "unused"
     */
    public final static int MANUFACTURER_ID = 0x45;

    /** */
    public boolean isDeviceSupported(SmafDevice.Info info) {
        for (SmafDevice.Info smafDeviceInfo : getDeviceInfo()) {
            if (smafDeviceInfo.equals(info)) {
                return true;
            }
        }
        return false;
    }

    /** */
    public SmafDevice.Info[] getDeviceInfo() {
        return new SmafDevice.Info[] {
                SmafSynthesizer.info,
                SmafSequencer.info,
                SmafMidiConverter.info
        };
    }

    /** */
    public SmafDevice getDevice(SmafDevice.Info info) throws IllegalArgumentException {
        if (info == SmafSynthesizer.info) {
            SmafSynthesizer synthesizer = new SmafSynthesizer();
            return synthesizer;
        } else if (info == SmafSequencer.info) {
            SmafSequencer sequencer = new SmafSequencer();
            return sequencer;
        } else if (info == SmafMidiConverter.info) {
            SmafMidiConverter converter = new SmafMidiConverter();
            return converter;
        } else {
            throw new IllegalArgumentException("info is not suitable for this provider");
        }
    }
}
