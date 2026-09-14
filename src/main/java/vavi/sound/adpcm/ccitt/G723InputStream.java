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
 * G723 InputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030828 nsano initial version <br>
 */
public class G723InputStream extends AdpcmInputStream {

    @Override
    protected Codec getCodec() {
        return switch (bits) {
            case 2 -> new G723_16();
            case 3 -> new G723_24();
            case 5 -> new G723_40();
            default -> throw new IllegalArgumentException("illegal bit: " + bits);
        };
    }

    /**
     * <li>TODO PCM encoding
     * {@link vavi.io.BitInputStream} is 2bit little endian fixed
     */
    public G723InputStream(InputStream in, ByteOrder byteOrder) {
        this(in, 2, ByteOrder.LITTLE_ENDIAN, byteOrder);
    }

    /**
     * <li>TODO PCM encoding
     * {@link vavi.io.BitInputStream} is 2bit fixed
     * @param byteOrder byte order for #read()
     */
    public G723InputStream(InputStream in, ByteOrder bitOrder, ByteOrder byteOrder) {
        this(in, 2, bitOrder, byteOrder);
    }

    /**
     * <li>TODO PCM encoding
     * @param bitOrder order of the packed two-bit ADPCM code words
     * @param byteOrder byte order for #read()
     */
    public G723InputStream(InputStream in, int bits, ByteOrder bitOrder, ByteOrder byteOrder) {
        super(in, byteOrder, bits, bitOrder);
        ((G723) decoder).setEncoding(encoding);
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
