/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.dvi;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmOutputStream;
import vavi.sound.adpcm.Codec;


/**
 * DVI OutputStream.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060124 nsano initial version <br>
 */
public class DviOutputStream extends AdpcmOutputStream {

    /** */
    protected Codec getCodec() {
        return new Dvi();
    }

    /**
     * {@link vavi.io.BitOutputStream} は 4bit big endian 固定
     * @param out ADPCM
     * @param byteOrder {@link #write(int)} のバイトオーダ
     */
    public DviOutputStream(OutputStream out, ByteOrder byteOrder) {
        super(out, byteOrder, 4, ByteOrder.BIG_ENDIAN);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }
}

/* */
