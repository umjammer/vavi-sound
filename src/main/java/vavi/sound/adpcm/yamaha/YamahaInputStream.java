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
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050402 nsano initial version <br>
 */
public class YamahaInputStream extends AdpcmInputStream {

    /** */
    protected Codec getCodec() {
        return new Yamaha();
    }

    /**
     * {@link vavi.io.BitInputStream} は 4bit little endian 固定
     * TODO ma は little endian ?
     */
    public YamahaInputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 4, ByteOrder.LITTLE_ENDIAN);
    }
}

/* */
