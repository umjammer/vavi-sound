/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.oki;

import java.io.InputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmInputStream;
import vavi.sound.adpcm.Codec;
import vavi.util.Debug;


/**
 * OKI InputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class OkiInputStream extends AdpcmInputStream {

    /** */
    protected Codec getCodec() {
        return new Oki();
    }

    /**
     * {@link vavi.io.BitInputStream} は 4bit little endian 固定
     * TODO PCM encoding
     */
    public OkiInputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 4, ByteOrder.LITTLE_ENDIAN); // oki adpcm は little endian 固定
Debug.println(this.in);
    }
}

/* */
