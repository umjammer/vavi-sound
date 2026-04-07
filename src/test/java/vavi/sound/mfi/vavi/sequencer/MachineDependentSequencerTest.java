/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import org.junit.jupiter.api.Test;

import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * MachineDependentSequencerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/04 umjammer initial version <br>
 */
class MachineDependentSequencerTest {

    @Test
    void test() {

        MachineDependentSequencer mds = MachineDependentSequencer.Factory.getSequencer(97);
Debug.println(mds);
        assertInstanceOf(vavi.sound.mfi.vavi.mitsubishi.MitsubishiSequencer.class, mds);

        mds = MachineDependentSequencer.Factory.getSequencer(17);
        assertInstanceOf(vavi.sound.mfi.vavi.nec.NecSequencer.class, mds);

        mds = MachineDependentSequencer.Factory.getSequencer(113);
        assertInstanceOf(vavi.sound.mfi.vavi.sharp.SharpSequencer.class, mds);

        assertThrows(IllegalArgumentException.class, () -> {
            MachineDependentSequencer.Factory.getSequencer(0);
        });

        // TODO recursive dependency
        mds = MachineDependentSequencer.Factory.getSequencer(33);
        assertInstanceOf(vavi.sound.mfi.vavi.fujitsu.FujitsuSequencer.class, mds);

        mds = MachineDependentSequencer.Factory.getSequencer(49);
        assertInstanceOf(vavi.sound.mfi.vavi.sony.SonySequencer.class, mds);

        mds = MachineDependentSequencer.Factory.getSequencer(65);
        assertInstanceOf(vavi.sound.mfi.vavi.panasonic.PanasonicSequencer.class, mds);
    }

    @Test
    void test2() throws Exception {
        MachineDependentFunction mdf = MachineDependentFunction.Factory.getFunction("96.1");
        assertInstanceOf(vavi.sound.mfi.vavi.mitsubishi.Function1.class, mdf);

        mdf = MachineDependentFunction.Factory.getFunction("16.240_1");
        assertInstanceOf(vavi.sound.mfi.vavi.nec.Function240_1.class, mdf);

        mdf = MachineDependentFunction.Factory.getFunction("112.129");
        assertInstanceOf(vavi.sound.mfi.vavi.sharp.Function129.class, mdf);

        mdf = MachineDependentFunction.Factory.getFunction("0.0");
        assertInstanceOf(vavi.sound.mfi.vavi.sequencer.UndefinedFunction.class, mdf);
    }
}
