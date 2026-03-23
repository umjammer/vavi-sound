/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import vavi.sound.mfi.vavi.header.VersMessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
}
