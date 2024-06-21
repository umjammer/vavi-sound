/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import org.junit.jupiter.api.Test;

import vavi.util.Debug;
import vavi.util.properties.PrefixedClassPropertiesFactory;
import vavi.util.properties.PrefixedPropertiesFactory;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * MachineDependentSequencerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/04 umjammer initial version <br>
 */
public class MachineDependentSequencerTest {

    @Test
    public void test() {
        PrefixedPropertiesFactory<Integer, MachineDependentSequencer> machineDependentSequencerFactory =
            new PrefixedClassPropertiesFactory<>("/vavi/sound/mfi/vavi/vavi.properties", "sequencer.vendor.");

        MachineDependentSequencer mds = machineDependentSequencerFactory.get(97);
Debug.println(mds);
        assertInstanceOf(vavi.sound.mfi.vavi.mitsubishi.MitsubishiSequencer.class, mds);

        mds = machineDependentSequencerFactory.get(17);
        assertInstanceOf(vavi.sound.mfi.vavi.nec.NecSequencer.class, mds);

        mds = machineDependentSequencerFactory.get(33);
        assertInstanceOf(vavi.sound.mfi.vavi.fujitsu.FujitsuSequencer.class, mds);

        mds = machineDependentSequencerFactory.get(49);
        assertInstanceOf(vavi.sound.mfi.vavi.sony.SonySequencer.class, mds);

        mds = machineDependentSequencerFactory.get(65);
        assertInstanceOf(vavi.sound.mfi.vavi.panasonic.PanasonicSequencer.class, mds);

        mds = machineDependentSequencerFactory.get(113);
        assertInstanceOf(vavi.sound.mfi.vavi.sharp.SharpSequencer.class, mds);
    }
}
