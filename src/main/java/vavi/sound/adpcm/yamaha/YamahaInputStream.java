/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.yamaha;

import java.io.InputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmInputStream;
import vavi.sound.adpcm.Codec;


/**
 * YAMAHA MA# InputStream
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050402 nsano initial version <br>
 */
public class YamahaInputStream extends AdpcmInputStream {

    @Override
    protected Codec getCodec() {
        return new Yamaha();
    }

    /**
     * {@link vavi.io.BitInputStream} is 4bit little endian fixed
     * TODO ma is little endian?
     */
    public YamahaInputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 4, ByteOrder.LITTLE_ENDIAN);
    }
}
