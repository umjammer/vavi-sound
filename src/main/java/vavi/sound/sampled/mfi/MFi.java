/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.mfi;

import java.util.Map;

import javax.sound.sampled.AudioFileFormat.Type;


/**
 * MFi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060125 nsano initial version <br>
 */
public class MFi extends Type {

    /**
     * @param properties keys are followings.
     * <pre>
     * "mfi.directory" String: output base directory
     * "mfi.base" String: output file template (use {@link String#format(String, Object...)})
     * "mfi.model" String: model
     * "mfi.time" float: dividing time in [sec]
     * "mfi.sampleRate" int: ADPCM sampling rate [Hz]
     * "mfi.bits" int: ADPCM sampling bits
     * "mfi.channels" int: ADPCM channels
     * "mfi.masterVolume" int: master volume in [%]
     * "mfi.adpcmVolume" int adpcm volume in [%]
     * </pre>
     */
    public MFi(Map<String, Object> properties) {
        super("Melody Format for i-mode", "mld");
        this.properties = properties;
    }

    /** */
    private Map<String, Object> properties;

    /** */
    public Map<String, Object> properties() {
        return properties;
    }

    /** */
    public Object getProperty(String key) {
        return properties.get(key);
    }
}
