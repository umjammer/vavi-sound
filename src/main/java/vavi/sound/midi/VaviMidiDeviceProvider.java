/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Properties;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;

import vavi.sound.midi.mfi.MfiSynthesizer;
import vavi.sound.midi.smaf.SmafSynthesizer;

import static java.lang.System.getLogger;


/**
 * VaviMidiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class VaviMidiDeviceProvider extends MidiDeviceProvider {

    private static final Logger logger = getLogger(VaviMidiDeviceProvider.class.getName());

    static {
        try {
            try (InputStream is = VaviMidiDeviceProvider.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound/pom.properties")) {
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

    public static final String version;

    /**
     * TODO used without asking
     * 0x45 is "unused"
     */
    public final static int MANUFACTURER_ID = 0x45;

    /** */
    private static final MidiDevice.Info[] infos = {
            MfiSynthesizer.info,
            SmafSynthesizer.info
    };

    @Override
    public MidiDevice.Info[] getDeviceInfo() {
        return infos;
    }

    /**
     * Returns a MIDI sequencer with an ADPCM playback mechanism.
     * @throws IllegalArgumentException info is not suitable for this provider
     */
    @Override
    public MidiDevice getDevice(MidiDevice.Info info) {

        if (info == MfiSynthesizer.info) {
logger.log(Level.DEBUG, "★1 info: " + info);
            MfiSynthesizer synthesizer = new MfiSynthesizer();
            return synthesizer;
        } else if (info == SmafSynthesizer.info) {
logger.log(Level.DEBUG, "★1 info: " + info);
            SmafSynthesizer synthesizer = new SmafSynthesizer();
            return synthesizer;
        } else {
logger.log(Level.DEBUG, "★1 not suitable for this provider: " + info);
            throw new IllegalArgumentException("info is not suitable for this provider");
        }
    }
}
