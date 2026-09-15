/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmOutputStream;
import vavi.sound.adpcm.Codec;


/**
 * G726 OutputStream
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260915 nsano initial version <br>
 */
public class G726OutputStream extends AdpcmOutputStream {

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
     * {@link vavi.io.BitOutputStream} is 4bit little endian fixed
     * <li> TODO endian for {@link vavi.io.BitOutputStream}
     * @param out ADPCM
     * @param byteOrder byte order for {@link #write(int)}
     */
    public G726OutputStream(OutputStream out, ByteOrder byteOrder) {
        this(out, 4, ByteOrder.LITTLE_ENDIAN, byteOrder);
    }

    /**
     * {@link vavi.io.BitOutputStream} is little endian fixed
     * <li> TODO endian for {@link vavi.io.BitOutputStream}
     * @param out ADPCM
     * @param bits 2, 3, 4, 5
     * @param byteOrder byte order for {@link #write(int)}
     */
    public G726OutputStream(OutputStream out, int bits, ByteOrder byteOrder) {
        this(out, bits, ByteOrder.LITTLE_ENDIAN, byteOrder);
    }

    /**
     * <li> TODO endian for {@link vavi.io.BitOutputStream}
     * @param out ADPCM
     * @param bits 2, 3, 4, 5
     * @param bitOrder code packing order for {@link vavi.io.BitOutputStream},
     *                 {@link ByteOrder#LITTLE_ENDIAN} for LSB first (RFC 3551, ffmpeg "g726le"),
     *                 {@link ByteOrder#BIG_ENDIAN} for MSB first (I.366.2 AAL2, ffmpeg "g726")
     * @param byteOrder byte order for {@link #write(int)}
     */
    public G726OutputStream(OutputStream out, int bits, ByteOrder bitOrder, ByteOrder byteOrder) {
        super(out, byteOrder, bits, bitOrder);
        ((G726) encoder).setEncoding(encoding);
    }
}
