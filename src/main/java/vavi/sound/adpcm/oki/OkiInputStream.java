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
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class OkiInputStream extends AdpcmInputStream {

    /** */
    protected Codec getCodec() {
        return new Oki();
    }

    /**
     * {@link vavi.io.BitInputStream} ‚Í 4bit little endian ŒÅ’è
     * TODO PCM encoding
     */
    public OkiInputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 4, ByteOrder.LITTLE_ENDIAN); // oki adpcm ‚Í little endian ŒÅ’è
        ((Oki) decoder).setEncoding(encoding);
Debug.println(this.in);
    }
}

/* */
