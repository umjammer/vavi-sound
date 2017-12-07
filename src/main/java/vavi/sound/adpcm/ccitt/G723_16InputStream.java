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

    /** */
    protected Codec getCodec() {
        return new G723_16();
    }

    /**
     * {@link vavi.io.BitInputStream} は 2bit little endian 固定
     * <li>TODO BitInputStream の endian
     * <li>TODO PCM encoding
     */
    public G723_16InputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 2, ByteOrder.LITTLE_ENDIAN);
        ((G723_16) decoder).setEncoding(encoding);
//Debug.println(this.in);
    }

    /** ADPCM (4bit) 換算時の長さ */
    public int available() throws IOException {
//Debug.println("0: " + in.available() + ", " + ((in.available() * 2) + (rest ? 1 : 0)));
        return (in.available() * 4) + (rest ? 1 : 0); // TODO check * 4 ???
    }
}

/* */
