/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmInputStream;
import vavi.sound.adpcm.Codec;


/**
 * G723_16 InputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030828 nsano initial version <br>
 */
public class G723_16InputStream extends AdpcmInputStream {

    @Override
    protected Codec getCodec() {
        return new G723_16();
    }

    /**
     * {@link vavi.io.BitInputStream} is 2bit little endian fixed
     * <li>TODO endian for BitInputStream
     * <li>TODO PCM encoding
     */
    public G723_16InputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 2, ByteOrder.LITTLE_ENDIAN);
        ((G723_16) decoder).setEncoding(encoding);
//logger.log(Level.DEBUG, this.in);
    }

    /** ADPCM (4bit) length */
    @Override
    public int available() throws IOException {
//logger.log(Level.DEBUG, "0: " + in.available() + ", " + ((in.available() * 2) + (rest ? 1 : 0)));
        return (in.available() * 4) + (rest ? 1 : 0); // TODO check * 4 ???
    }
}
