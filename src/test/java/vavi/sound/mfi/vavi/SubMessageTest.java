/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.ByteArrayInputStream;

import vavi.sound.mfi.vavi.audio.AdpmMessage;
import vavi.sound.mfi.vavi.header.VersMessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * SubMessageTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-19 nsano initial version <br>
 */
class SubMessageTest {

    @Test
    void test() throws Exception {
        SubMessage sm = SubMessage.factory("vers");
        assertInstanceOf(VersMessage.class, sm);

        sm = SubMessage.factory("????");
        assertNull(sm);
    }

    @Test
    void createsIndependentProviderInstances() throws Exception {
        byte[] adpm4 = {'a', 'd', 'p', 'm', 0, 3, 16, 4, 1};
        byte[] adpm2 = {'a', 'd', 'p', 'm', 0, 3, 32, 2, 1};
        SubMessage first = SubMessage.readFrom(new ByteArrayInputStream(adpm4));
        SubMessage second = SubMessage.readFrom(new ByteArrayInputStream(adpm2));

        assertNotSame(first, second);
        assertEquals(4, ((AdpmMessage) first).getSamplingBits());
        assertEquals(2, ((AdpmMessage) second).getSamplingBits());
    }
}
