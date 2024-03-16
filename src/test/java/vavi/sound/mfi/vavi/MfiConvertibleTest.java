/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * MfiConvertibleTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/16 umjammer initial version <br>
 */
class MfiConvertibleTest {

    @Test
    void test() {
        // exists in /vavi/sound/mfi/vavi/vavi.properties
        MfiConvertible converter = MfiConvertible.factory.get("short.144");
        assertInstanceOf(VaviNoteMessage.class, converter);
        // not exists
        converter = MfiConvertible.factory.get("meta.144");
        assertNull(converter);
    }
}
