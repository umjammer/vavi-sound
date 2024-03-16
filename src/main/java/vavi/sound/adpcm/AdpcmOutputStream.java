/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;

import vavi.io.BitOutputStream;


/**
 * AdpcmOutputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030827 nsano initial version <br>
 *          0.10 060427 nsano refactoring <br>
 */
public abstract class AdpcmOutputStream extends FilterOutputStream {

    /** PCM format for #write(int) */
    protected AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;

    /** PCM byte order for #write(int) */
    protected ByteOrder byteOrder;

    /** encoder */
    protected Codec encoder;

    /** */
    protected abstract Codec getCodec();

    /**
     * TODO make gathered all BitOutputStream arguments BitOutputStream?
     * @param out ADPCM
     * @param byteOrder byte order for {@link #write(int)}
     * @param bits {@link BitOutputStream} size
     * @param bitOrder byte order for {@link BitOutputStream}
     */
    public AdpcmOutputStream(OutputStream out, ByteOrder byteOrder, int bits, ByteOrder bitOrder) {
        super(new BitOutputStream(out, bits, bitOrder));
        this.byteOrder = byteOrder;
        this.encoder = getCodec();
//Debug.println(this.out);
    }

    /** remaining or not (having PCM L or H at least one side) */
    protected boolean flushed = true;
    /** current value (when PCM L or H one part is coming) */
    protected int current;

    /**
     * @param b let "PCM H or L byte" {@link #byteOrder} order (LSB 8bit available)
     */
    @Override
    public void write(int b) throws IOException {
        if (!flushed) {

            if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                current |= b & 0x00ff;
            } else {
                current |= (b & 0x00ff) << 8;
            }

            if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
                if ((current & 0x8000) != 0) {
                    current -= 0x10000;
                }
            }
//System.err.println("current: " + StringUtil.toHex4(current));
            out.write(encoder.encode(current)); // BitOutputStream write 4bit

            flushed = true;
        } else {
            if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                current = (b & 0xff) << 8;
            } else {
                current = b & 0xff;
            }

            flushed = false;
        }
    }
}
