/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ServiceLoader;

import vavi.sound.adpcm.AdpcmInputStreamFactory;
import vavi.sound.adpcm.ccitt.G721InputStream;
import vavi.sound.adpcm.dvi.DviInputStream;
import vavi.sound.adpcm.oki.OkiInputStream;
import vavi.sound.adpcm.vox.VoxInputStream;
import vavi.sound.adpcm.yamaha.YamahaInputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-05 nsano initial version <br>
 */
class TestCase {

    @Test
    void test1() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[0]);

        Assertions.assertInstanceOf(G721InputStream.class, inputStream("g721", is));
        Assertions.assertInstanceOf(YamahaInputStream.class, inputStream("yamaha", is));
        Assertions.assertInstanceOf(VoxInputStream.class, inputStream("vox", is));
        Assertions.assertInstanceOf(OkiInputStream.class, inputStream("oki", is));
        Assertions.assertInstanceOf(DviInputStream.class, inputStream("dvi", is));
        Assertions.assertThrows(IllegalArgumentException.class, () -> inputStream("toyota", is));
    }

    static InputStream inputStream(String decoder, InputStream in) {
        for (AdpcmInputStreamFactory ais : ServiceLoader.load(AdpcmInputStreamFactory.class)) {
            if (ais.getClass().getName().toLowerCase().contains(decoder)) {
                return ais.factory(in);
            }
        }
        throw new IllegalArgumentException("unsupported 4-bit decoder: " + decoder);
    }
}
