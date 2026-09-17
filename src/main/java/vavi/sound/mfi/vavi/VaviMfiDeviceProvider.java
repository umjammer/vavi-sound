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
import vavi.sound.mfi.spi.MfiMidiConverter;

import static java.lang.System.getLogger;


/**
 * {@link MfiDeviceProvider} implemented by vavi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020629 nsano initial version <br>
 *          0.10 020703 nsano complete <br>
 *          0.11 030819 nsano add {@link MfiMidiConverter} <br>
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

    /**
     * TODO used without asking
     * 0x45 is "unused"
     */
    public final static int MANUFACTURER_ID = 0x45;

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
                VaviMfiSequencer.info,
                VaviMfiSynthesizer.info
        };
    }

    @Override
    public MfiDevice getDevice(MfiDevice.Info info) {
        if (info == VaviMfiSynthesizer.info) {
            VaviMfiSynthesizer synthesizer = new VaviMfiSynthesizer();
            return synthesizer;
        } else if (info == VaviMfiSequencer.info) {
            VaviMfiSequencer sequencer = new VaviMfiSequencer();
            return sequencer;
        } else {
            throw new IllegalArgumentException("info is not suitable for this provider");
        }
    }
}
