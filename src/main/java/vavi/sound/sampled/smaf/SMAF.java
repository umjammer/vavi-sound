/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.smaf;

import java.util.Map;

import javax.sound.sampled.AudioFileFormat.Type;


/**
 * SMAF.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080415 nsano initial version <br>
 */
public class SMAF extends Type {

    /**
     * @param properties keys are followings.
     * <pre>
     * "smaf.directory" String: output base directory, after this "mmf" directory will be added
     * "smaf.base" String: output file template (use {@link String#format(String, Object...)}) need extension, like "foo_%d.mmf"
     * "smaf.time" float: dividing time in [sec]
     * "smaf.sampleRate" int: ADPCM sampling rate [Hz]
     * "smaf.bits" int: ADPCM sampling bits
     * "smaf.channels" int: ADPCM channels
     * "smaf.masterVolume" int: master volume in [%]
     * "smaf.adpcmVolume" int adpcm volume in [%]
     * </pre>
     */
    public SMAF(Map<String, Object> properties) {
        super("Synthetic music Mobile Application Format", "mmf");
        this.properties = properties;
    }

    /** */
    private final Map<String, Object> properties;

    /** */
    public Map<String, Object> properties() {
        return properties;
    }

    /** */
    public Object getProperty(String key) {
        return properties.get(key);
    }
}
