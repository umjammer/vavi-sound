/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 */

package vavi.sound.mfi.vavi.track;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class Nop2MessageTest {

    @Test
    void twoByteDelta() {
        Nop2Message message = new Nop2Message().init(0x53, 0x01);
        assertEquals(0x0153, message.getDelta());
    }
}
