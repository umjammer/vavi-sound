/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.properties.PrefixedPropertiesFactory;


/**
 * Sub sequencer for machine dependent system exclusive message.
 * <p>
 * 今のところ実装クラスは bean でなければならない。
 * (引数なしのコンストラクタがあること)
 * {@link #process(MachineDependentMessage)} 関連はステートレスでなければならない。
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public interface MachineDependentFunction {

    final int VENDOR_NEC        = 0x10; // N
    final int VENDOR_FUJITSU    = 0x20; // F
    final int VENDOR_SONY       = 0x30; // SO
    final int VENDOR_PANASONIC  = 0x40; // P
    final int VENDOR_NIHONMUSEN = 0x50; // R
    final int VENDOR_MITSUBISHI = 0x60; // D
    final int VENDOR_SHARP      = 0x70; // SH
    final int VENDOR_SANYO      = 0x80; // SA
    final int VENDOR_MOTOROLA   = 0x90; // M

    final int CARRIER_AU     = 0x00;    // au
    final int CARRIER_DOCOMO = 0x01;    // DoCoMo

    /** */
    void process(MachineDependentMessage message)
        throws InvalidMfiDataException;

    /** */
    class Factory extends PrefixedPropertiesFactory<String, MachineDependentFunction> {

        /** */
        public Factory(String path) {
            super(path, "function.");
        }

        /** */
        public MachineDependentFunction getFunction(String key) {
            try {
                return get(key);
            } catch (IllegalArgumentException e) {
                return new UndefinedFunction(); // TODO should throw exception
            }
        }
    }
}

/* */
