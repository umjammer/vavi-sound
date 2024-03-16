/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import java.io.InputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmInputStream;
import vavi.sound.adpcm.Codec;


/**
 * G721 InputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030714 nsano initial version <br>
 *          0.01 030714 nsano fine tune <br>
 *          0.02 030714 nsano fix available() <br>
 *          0.03 030715 nsano support read() endian <br>
 *          0.10 060427 nsano refactoring <br>
 */
public class G721InputStream extends AdpcmInputStream {

    @Override
    protected Codec getCodec() {
        return new G721();
    }

    /**
     * {@link vavi.io.BitInputStream} is 4bit little endian fixed
     * <li>TODO endian for BitInputStream
     * <li>TODO PCM encoding
     * @param in G721 ADPCM
     * @param byteOrder byte order for #read()
     */
    public G721InputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 4, ByteOrder.LITTLE_ENDIAN);
        ((G721) decoder).setEncoding(encoding);
    }
}
