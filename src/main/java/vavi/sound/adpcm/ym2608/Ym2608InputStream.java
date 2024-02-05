/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ym2608;

import java.io.InputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmInputStream;
import vavi.sound.adpcm.Codec;


/**
 * YAMAHA (YM2608) InputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030816 nsano initial version <br>
 */
public class Ym2608InputStream extends AdpcmInputStream {

    @Override
    protected Codec getCodec() {
        return new Ym2608();
    }

    /**
     * {@link vavi.io.BitInputStream} は 4bit big endian 固定
     */
    public Ym2608InputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 4, ByteOrder.BIG_ENDIAN);
    }
}
