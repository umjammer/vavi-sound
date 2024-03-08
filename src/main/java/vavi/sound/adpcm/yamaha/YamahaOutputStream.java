/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.yamaha;

import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmOutputStream;
import vavi.sound.adpcm.Codec;


/**
 * YAMAHA MA# OutputStream
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050402 nsano initial version <br>
 */
public class YamahaOutputStream extends AdpcmOutputStream {

    @Override
    protected Codec getCodec() {
        return new Yamaha();
    }

    /**
     *
     * {@link vavi.io.BitOutputStream} is 4bit little endian fixed
     * @param out ADPCM
     * @param byteOrder byte order for {@link #write(int)}
     */
    public YamahaOutputStream(OutputStream out, ByteOrder byteOrder) {
        super(out, byteOrder, 4, ByteOrder.LITTLE_ENDIAN);
    }
}
