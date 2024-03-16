/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmOutputStream;
import vavi.sound.adpcm.Codec;


/**
 * G721 OutputStream
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030827 nsano initial version <br>
 *          0.10 060427 nsano refactoring <br>
 */
public class G721OutputStream extends AdpcmOutputStream {

    @Override
    protected Codec getCodec() {
        return new G721();
    }

    /**
     * {@link vavi.io.BitOutputStream} is 4bit little endian fixed
     * <li> TODO endian for {@link vavi.io.BitOutputStream}
     * @param out ADPCM
     * @param byteOrder byte order for {@link #write(int)}
     */
    public G721OutputStream(OutputStream out, ByteOrder byteOrder) {
        super(out, byteOrder, 4, ByteOrder.LITTLE_ENDIAN);
        ((G721) encoder).setEncoding(encoding);
    }
}
