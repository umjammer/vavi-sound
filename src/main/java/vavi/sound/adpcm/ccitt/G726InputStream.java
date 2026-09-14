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
        this(in, 4, byteOrder);
    }

    /**
     * {@link vavi.io.BitInputStream} is little endian (RFC 3551, LSB first)
     * <li>TODO PCM encoding
     * @param in G726 ADPCM
     * @param bits 2, 3, 4, 5
     * @param byteOrder byte order for #read()
     */
    public G726InputStream(InputStream in, int bits, ByteOrder byteOrder) {
        this(in, bits, ByteOrder.LITTLE_ENDIAN, byteOrder);
    }

    /**
     * <li>TODO PCM encoding
     * @param in G726 ADPCM
     * @param bits 2, 3, 4, 5
     * @param bitOrder code packing order for {@link vavi.io.BitInputStream},
     *                 {@link ByteOrder#LITTLE_ENDIAN} for LSB first (RFC 3551, ffmpeg "g726le"),
     *                 {@link ByteOrder#BIG_ENDIAN} for MSB first (I.366.2 AAL2, ffmpeg "g726")
     * @param byteOrder byte order for #read()
     */
    public G726InputStream(InputStream in, int bits, ByteOrder bitOrder, ByteOrder byteOrder) {
        super(in, byteOrder, bits, bitOrder);
        ((G726) decoder).setEncoding(encoding);
    }

    /** */
    public static class G726InputStreamFactory implements AdpcmInputStreamFactory {
        @Override public AdpcmInputStream factory(InputStream in) {
            return new G726InputStream(in, ByteOrder.LITTLE_ENDIAN);
        }
    }
}
