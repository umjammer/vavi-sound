/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;

import static java.lang.System.getLogger;


/**
 * Sub sequencer for machine dependent system exclusive message.
 * <p>
 * Currently, an implementation class of this interface must be an bean.
 * (means having a contractor without argument)
 * {@link #process(MachineDependentMessage)} related must be state less.
 * </p>
 * <pre>
 * properties file ... any
 * name prefix ... "function."
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public interface MachineDependentFunction {

    Logger logger = getLogger(MachineDependentFunction.class.getName());

    int VENDOR_FUJITSU    = 0x20; // F
    int VENDOR_SONY       = 0x30; // SO
    int VENDOR_PANASONIC  = 0x40; // P
    int VENDOR_NIHONMUSEN = 0x50; // R
    int VENDOR_SANYO      = 0x80; // SA
    int VENDOR_MOTOROLA   = 0x90; // M

    int CARRIER_AU     = 0x00;    // au
    int CARRIER_DOCOMO = 0x01;    // DoCoMo

    /** */
    String getId();

    /** */
    void process(MachineDependentMessage message) throws InvalidMfiDataException;

    /** factory */
    class Factory {

        static final Map<String, MachineDependentFunction> functions = new HashMap<>();

        /** */
        public static MachineDependentFunction getFunction(String key) {
            MachineDependentFunction function = functions.get(key);
            if (function == null) {
logger.log(Level.WARNING, "no matched machine dependent function for: " + key);
                return new UndefinedFunction(); // TODO should throw exception or not?
            }
            return function;
        }

        static {
            for (MachineDependentFunction function : ServiceLoader.load(MachineDependentFunction.class)) {
                functions.put(function.getId(), function);
            }
        }
    }
}
