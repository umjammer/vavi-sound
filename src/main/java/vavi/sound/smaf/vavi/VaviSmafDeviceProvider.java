/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi;

import java.io.InputStream;
import java.lang.System.Logger;
import java.util.Properties;

import vavi.sound.smaf.SmafDevice;
import vavi.sound.smaf.SmafDevice.Info;
import vavi.sound.smaf.spi.SmafDeviceProvider;

import static java.lang.System.getLogger;


/**
 * {@link VaviSmafDeviceProvider} implemented by vavi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071012 nsano initial version <br>
 */
public class VaviSmafDeviceProvider extends SmafDeviceProvider {

    private static final Logger logger = getLogger(VaviSmafDeviceProvider.class.getName());

    static {
        try {
            try (InputStream is = VaviSmafDeviceProvider.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound/pom.properties")) {
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

    @Override
    public boolean isDeviceSupported(Info info) {
        for (Info smafDeviceInfo : getDeviceInfo()) {
            if (smafDeviceInfo.equals(info)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Info[] getDeviceInfo() {
        return new Info[] {
                VaviSmafSynthesizer.info,
                VaviSmafSequencer.info
        };
    }

    @Override
    public SmafDevice getDevice(SmafDevice.Info info) throws IllegalArgumentException {
        if (info == VaviSmafSynthesizer.info) {
            VaviSmafSynthesizer synthesizer = new VaviSmafSynthesizer();
            return synthesizer;
        } else if (info == VaviSmafSequencer.info) {
            VaviSmafSequencer sequencer = new VaviSmafSequencer();
            return sequencer;
        } else {
            throw new IllegalArgumentException("info is not suitable for this provider");
        }
    }
}
