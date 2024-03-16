/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ym2608;

import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmOutputStream;
import vavi.sound.adpcm.Codec;


/**
 * YAMAHA (YM2608) OutputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030827 nsano initial version <br>
 */
public class Ym2608OutputStream extends AdpcmOutputStream {

    @Override
    protected Codec getCodec() {
        return new Ym2608();
    }

    /**
     * {@link vavi.io.BitOutputStream} is 4bit big endian fixed
     * @param out ADPCM
     * @param byteOrder byte order for #write(int)
     */
    public Ym2608OutputStream(OutputStream out, ByteOrder byteOrder) {
        super(out, byteOrder, 4, ByteOrder.BIG_ENDIAN);
    }
}
