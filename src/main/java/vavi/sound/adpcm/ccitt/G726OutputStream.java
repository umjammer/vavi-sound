/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmOutputStream;
import vavi.sound.adpcm.Codec;


/**
 * G726 OutputStream
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260915 nsano initial version <br>
 */
public class G726OutputStream extends AdpcmOutputStream {

    @Override
    protected Codec getCodec() {
        return new G726_32();
    }

    /**
     * {@link vavi.io.BitOutputStream} is 4bit little endian fixed
     * <li> TODO endian for {@link vavi.io.BitOutputStream}
     * @param out ADPCM
     * @param byteOrder byte order for {@link #write(int)}
     */
    public G726OutputStream(OutputStream out, ByteOrder byteOrder) {
        super(out, byteOrder, 4, ByteOrder.LITTLE_ENDIAN);
        ((G726) encoder).setEncoding(encoding);
    }
}
