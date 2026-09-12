/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import javax.sound.midi.MidiEvent;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;


/**
 * SmafConvertible.
 * <p>
 * currently the implementation class must be a bean.
 * (have a no-argument constructor)
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public interface SmafConvertible {

    Logger logger = System.getLogger(SmafConvertible.class.getName());

    /** Whether this converts the MIDI message {@code key} stands for. */
    boolean accept(String key);

    /** TODO the implementation method is not good enough, use BeanUtil etc.? */
    SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context) throws InvalidSmafDataException;

    /** what {@link #getConvertible(String)} has found so far, a null value is "there is none" */
    Map<String, SmafConvertible> convertibles = new HashMap<>();

    /**
     * factory
     *
     * @param key "short.#", "short.176.#" or "meta.#"
     * @return nullable
     * @see "/META-INF/services/vavi.sound.smaf.vavi.message.SmafConvertible"
     */
    static SmafConvertible getConvertible(String key) {
        if (!convertibles.containsKey(key)) {
            SmafConvertible found = null;
            for (SmafConvertible convertible : ServiceLoader.load(SmafConvertible.class)) {
                if (convertible.accept(key)) {
                    found = convertible;
                    break;
                }
            }
            if (found == null) {
                logger.log(Level.DEBUG, "no convertible found for: " + key);
            }
            convertibles.put(key, found);
        }
        return convertibles.get(key);
    }
}
