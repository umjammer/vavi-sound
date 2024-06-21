/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.properties.PrefixedClassPropertiesFactory;

import static java.lang.System.getLogger;


/**
 * Sub sequencer for machine dependent system exclusive message.
 * <p>
 * Currently, an implementation class of this interface should be an bean.
 * (means having a contractor without argument)
 * {@link #process(MachineDependentMessage)} related should be state less.
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

    int VENDOR_NEC        = 0x10; // N
    int VENDOR_FUJITSU    = 0x20; // F
    int VENDOR_SONY       = 0x30; // SO
    int VENDOR_PANASONIC  = 0x40; // P
    int VENDOR_NIHONMUSEN = 0x50; // R
    int VENDOR_MITSUBISHI = 0x60; // D
    int VENDOR_SHARP      = 0x70; // SH
    int VENDOR_SANYO      = 0x80; // SA
    int VENDOR_MOTOROLA   = 0x90; // M

    int CARRIER_AU     = 0x00;    // au
    int CARRIER_DOCOMO = 0x01;    // DoCoMo

    /** */
    void process(MachineDependentMessage message)
        throws InvalidMfiDataException;

    /** factory */
    class Factory extends PrefixedClassPropertiesFactory<String, MachineDependentFunction> {

        /** */
        public Factory(String path) {
            super(path, "function.");
        }

        /** */
        public MachineDependentFunction getFunction(String key) {
            try {
                return get(key);
            } catch (IllegalArgumentException e) {
logger.log(Level.WARNING, key);
logger.log(Level.WARNING, e.getMessage(), e);
                return new UndefinedFunction(); // TODO should throw exception or not?
            }
        }
    }
}
