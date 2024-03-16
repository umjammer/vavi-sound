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
        assertTrue(mds instanceof vavi.sound.mfi.vavi.mitsubishi.MitsubishiSequencer);

        mds = machineDependentSequencerFactory.get(17);
        assertTrue(mds instanceof vavi.sound.mfi.vavi.nec.NecSequencer);

        mds = machineDependentSequencerFactory.get(33);
        assertTrue(mds instanceof vavi.sound.mfi.vavi.fujitsu.FujitsuSequencer);

        mds = machineDependentSequencerFactory.get(49);
        assertTrue(mds instanceof vavi.sound.mfi.vavi.sony.SonySequencer);

        mds = machineDependentSequencerFactory.get(65);
        assertTrue(mds instanceof vavi.sound.mfi.vavi.panasonic.PanasonicSequencer);

        mds = machineDependentSequencerFactory.get(113);
        assertTrue(mds instanceof vavi.sound.mfi.vavi.sharp.SharpSequencer);
    }
}
