/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.misc;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Properties;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.spi.MixerProvider;

import vavi.sound.mfi.vavi.VaviMfiDeviceProvider;

import static java.lang.System.getLogger;


/**
 * MiscMixerProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-19 nsano initial version <br>
 */
public class MiscMixerProvider extends MixerProvider {

    private static final Logger logger = getLogger(VaviMfiDeviceProvider.class.getName());

    static {
        try {
            try (InputStream is = MiscMixerProvider.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound/pom.properties")) {
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
    public Info[] getMixerInfo() {
        return new Info[] {
                WaveOutMixer.mixerInfo,
                NullMixer.mixerInfo,
                HijackMixer.mixerInfo
        };
    }

    @Override
    public Mixer getMixer(Info info) {
        if (info == WaveOutMixer.mixerInfo) {
logger.log(Level.DEBUG, "★1 info: " + info);
            WaveOutMixer mixer = new WaveOutMixer();
            return mixer;
        } else if (info == NullMixer.mixerInfo) {
logger.log(Level.DEBUG, "★1 info: " + info);
            NullMixer mixer = new NullMixer();
            return mixer;
        } else if (info == HijackMixer.mixerInfo) {
logger.log(Level.DEBUG, "★1 info: " + info);
            HijackMixer mixer = new HijackMixer();
            return mixer;
        } else {
logger.log(Level.DEBUG, "not suitable for this provider: " + info);
            throw new IllegalArgumentException("info is not suitable for this provider");
        }
    }
}
