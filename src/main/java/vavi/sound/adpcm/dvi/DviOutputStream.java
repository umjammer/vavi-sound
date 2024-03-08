/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.dvi;

import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmOutputStream;
import vavi.sound.adpcm.Codec;


/**
 * DVI OutputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060124 nsano initial version <br>
 */
public class DviOutputStream extends AdpcmOutputStream {

    @Override
    protected Codec getCodec() {
        return new Dvi();
    }

    /**
     * {@link vavi.io.BitOutputStream} is 4bit big endian fixed
     * @param out ADPCM
     * @param byteOrder byte order for {@link #write(int)}
     */
    public DviOutputStream(OutputStream out, ByteOrder byteOrder) {
        super(out, byteOrder, 4, ByteOrder.BIG_ENDIAN);
    }
}
