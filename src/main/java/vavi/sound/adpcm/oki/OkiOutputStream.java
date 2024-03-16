/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.oki;

import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmOutputStream;
import vavi.sound.adpcm.Codec;


/**
 * OKI OutputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060122 nsano initial version <br>
 */
public class OkiOutputStream extends AdpcmOutputStream {

    @Override
    protected Codec getCodec() {
        return new Oki();
    }

    /**
     * {@link vavi.io.BitOutputStream} is 4bit little endian fixed
     * @param out ADPCM
     * @param byteOrder byte order for #write(int)
     */
    public OkiOutputStream(OutputStream out, ByteOrder byteOrder) {
        super(out, byteOrder, 4, ByteOrder.LITTLE_ENDIAN);
    }
}
