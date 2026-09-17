/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.ByteArrayInputStream;

import vavi.sound.mfi.vavi.audio.AdpmChunk;
import vavi.sound.mfi.vavi.sub.VersChunk;

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
class SubChunkTest {

    @Test
    void test() throws Exception {
        SubChunk sm = SubChunk.factory("vers");
        assertInstanceOf(VersChunk.class, sm);

        sm = SubChunk.factory("????");
        assertNull(sm);
    }

    @Test
    void createsIndependentProviderInstances() throws Exception {
        byte[] adpm4 = {'a', 'd', 'p', 'm', 0, 3, 16, 4, 1};
        byte[] adpm2 = {'a', 'd', 'p', 'm', 0, 3, 32, 2, 1};
        SubChunk first = SubChunk.readFrom(new ByteArrayInputStream(adpm4));
        SubChunk second = SubChunk.readFrom(new ByteArrayInputStream(adpm2));

        assertNotSame(first, second);
        assertEquals(4, ((AdpmChunk) first).getSamplingBits());
        assertEquals(2, ((AdpmChunk) second).getSamplingBits());
    }
}
