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
 * G723 OutputStream
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260915 nsano initial version <br>
 */
public class G723OutputStream extends AdpcmOutputStream {

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
     * {@link vavi.io.BitOutputStream} is 2bit little endian fixed
     * <li> TODO endian for {@link vavi.io.BitOutputStream}
     * @param out ADPCM
     * @param byteOrder byte order for {@link #write(int)}
     */
    public G723OutputStream(OutputStream out, ByteOrder byteOrder) {
        this(out, 2, ByteOrder.LITTLE_ENDIAN, byteOrder);
    }

    /**
     * {@link vavi.io.BitOutputStream} is 2bit fixed
     * <li> TODO endian for {@link vavi.io.BitOutputStream}
     * @param out ADPCM
     * @param bitOrder code packing order for {@link vavi.io.BitOutputStream},
     *                 {@link ByteOrder#LITTLE_ENDIAN} for LSB first (RFC 3551, ffmpeg "g726le"),
     *                 {@link ByteOrder#BIG_ENDIAN} for MSB first (I.366.2 AAL2, ffmpeg "g726")
     * @param byteOrder byte order for {@link #write(int)}
     */
    public G723OutputStream(OutputStream out, ByteOrder bitOrder, ByteOrder byteOrder) {
        this(out, 2, bitOrder, byteOrder);
    }

    /**
     * <li> TODO endian for {@link vavi.io.BitOutputStream}
     * @param out ADPCM
     * @param bits 2, 3, 5
     * @param bitOrder code packing order for {@link vavi.io.BitOutputStream},
     *                 {@link ByteOrder#LITTLE_ENDIAN} for LSB first (RFC 3551, ffmpeg "g726le"),
     *                 {@link ByteOrder#BIG_ENDIAN} for MSB first (I.366.2 AAL2, ffmpeg "g726")
     * @param byteOrder byte order for {@link #write(int)}
     */
    public G723OutputStream(OutputStream out, int bits, ByteOrder bitOrder, ByteOrder byteOrder) {
        super(out, byteOrder, bits, bitOrder);
        ((G723) encoder).setEncoding(encoding);
    }
}
