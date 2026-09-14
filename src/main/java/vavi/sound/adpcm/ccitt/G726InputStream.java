/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import java.io.InputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmInputStream;
import vavi.sound.adpcm.AdpcmInputStreamFactory;
import vavi.sound.adpcm.Codec;


/**
 * G726 InputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260915 nsano initial version <br>
 */
public class G726InputStream extends AdpcmInputStream {

    @Override
    protected Codec getCodec() {
        return switch (bits) {
            case 2 -> new G726_16();
            case 3 -> new G726_24();
            case 4 -> new G726_32();
            case 5 -> new G726_40();
            default -> throw new IllegalArgumentException("illegal bit: " + bits);
        };
    }

    /**
     * {@link vavi.io.BitInputStream} is 4bit little endian fixed
     * <li>TODO endian for BitInputStream
     * <li>TODO PCM encoding
     * @param in G721 ADPCM
     * @param byteOrder byte order for #read()
     */
    public G726InputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 4, ByteOrder.LITTLE_ENDIAN);
        ((G726) decoder).setEncoding(encoding);
    }

    /** */
    public static class G726InputStreamFactory implements AdpcmInputStreamFactory {
        @Override public AdpcmInputStream factory(InputStream in) {
            return new G726InputStream(in, ByteOrder.LITTLE_ENDIAN);
        }
    }
}
