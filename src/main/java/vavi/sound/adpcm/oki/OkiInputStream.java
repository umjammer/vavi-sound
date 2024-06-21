/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.oki;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmInputStream;
import vavi.sound.adpcm.Codec;

import static java.lang.System.getLogger;


/**
 * OKI InputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class OkiInputStream extends AdpcmInputStream {

    private static final Logger logger = getLogger(OkiInputStream.class.getName());

    @Override
    protected Codec getCodec() {
        return new Oki();
    }

    /**
     * {@link vavi.io.BitInputStream} is 4bit little endian fixed
     * TODO PCM encoding
     */
    public OkiInputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 4, ByteOrder.LITTLE_ENDIAN); // oki adpcm is little endian fixed
logger.log(Level.DEBUG, this.in);
    }
}
