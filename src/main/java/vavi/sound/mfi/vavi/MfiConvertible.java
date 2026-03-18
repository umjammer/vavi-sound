/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;


/**
 * MfiConvertible
 * <p>
 * Currently, an implementation class of this interface should be an bean.
 * (means having a contractor without argument)
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030905 nsano initial version <br>
 */
public interface MfiConvertible {

    Logger logger = System.getLogger(MfiConvertible.class.getName());

    /** caution, name is conflicted with {@link TrackMessage#accept(String)} */
    boolean accept(String key);

    /** */
    MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException;

    /** factory */
    Map<String, MfiConvertible> convertibles = new HashMap<>();

    /**
     * @param key "short.#" or "meta.#"
     * @return nullable
     */
    static MfiConvertible getConvertible(String key) {
        for (MfiConvertible convertible : ServiceLoader.load(MfiConvertible.class)) {
            if (convertible.accept(key)) {
                return convertible;
            }
        }
logger.log(Level.WARNING, "no convertible found for: " + key);
        return null;
    }
}
