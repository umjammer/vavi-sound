/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import org.junit.Ignore;
import org.junit.Test;

import vavi.util.properties.PrefixedPropertiesFactory;

import static org.junit.Assert.assertTrue;


/**
 * MachineDependentSequencerTest. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/04 umjammer initial version <br>
 */
@Ignore // TODO
public class MachineDependentSequencerTest {

    @Test
    public void test() {
        PrefixedPropertiesFactory<Integer, MachineDependentSequencer> machineDependentSequencerFactory =
            new PrefixedPropertiesFactory<>("/vavi/sound/mfi/vavi/vavi.properties", "sequencer.vendor.");

        MachineDependentSequencer mds = machineDependentSequencerFactory.get(97);
        System.err.println(mds);
        assertTrue(vavi.sound.mfi.vavi.mitsubishi.MitsubishiSequencer.class.isInstance(mds));

//        mds = machineDependentSequencerFactory.get(17);
//        assertTrue(vavi.sound.mfi.vavi.nec.NecSequencer.class.isInstance(mds));

//        mds = machineDependentSequencerFactory.get(33);
//        assertTrue(org.klab.packetcast.mfi.fujitsu.FujitsuSequencer.class.isInstance(mds));

//        mds = machineDependentSequencerFactory.get(49);
//        assertTrue(org.klab.packetcast.mfi.sony.SonySequencer.class.isInstance(mds));

//        mds = machineDependentSequencerFactory.get(65);
//        assertTrue(org.klab.packetcast.mfi.panasonic.PanasonicSequencer.class.isInstance(mds));

//        mds = machineDependentSequencerFactory.get(113);
//        assertTrue(vavi.sound.mfi.vavi.sharp.SharpSequencer.class.isInstance(mds));
    }
}

/* */
