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
     */
    public G723_16InputStream(InputStream in, ByteOrder byteOrder) {
        this(in, byteOrder, ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * @param bitOrder order of the packed two-bit ADPCM code words
     */
    public G723_16InputStream(InputStream in, ByteOrder byteOrder, ByteOrder bitOrder) {
        super(in, byteOrder, 2, bitOrder);
        ((G723_16) decoder).setEncoding(encoding);
//logger.log(Level.TRACE, this.in);
    }

    /**
     * Number of PCM bytes that can be read without blocking.
     *
     * <p>{@link vavi.io.BitInputStream#available()} already reports the
     * number of two-bit code words (four per input byte).  Each decoded code
     * word produces one 16-bit PCM sample, so the normal ADPCM calculation in
     * {@link AdpcmInputStream} is the correct one here.  The old override
     * multiplied that value by four once more, causing the MFi player to run
     * past the real end of short Type-2 streams and to reuse stale bytes at
     * their boundaries.</p>
     */
    @Override
    public int available() throws IOException {
        return super.available();
    }
}
