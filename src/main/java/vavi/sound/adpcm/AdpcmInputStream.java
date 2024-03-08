/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;

import vavi.io.BitInputStream;
import vavi.io.BitOutputStream;
import vavi.util.Debug;


/**
 * AdpcmInputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030714 nsano initial version <br>
 *          0.01 030714 nsano fine tune <br>
 *          0.02 030714 nsano fix available() <br>
 *          0.03 030715 nsano support read() endian <br>
 *          0.10 060427 nsano refactoring <br>
 */
public abstract class AdpcmInputStream extends FilterInputStream {

    /** PCM format that #read() returns */
    protected AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;

    /** PCM byte order that #read() returns */
    protected ByteOrder byteOrder;

    /** decoder */
    protected Codec decoder;

    /** */
    protected abstract Codec getCodec();

    /**
     * @param in PCM
     * @param byteOrder byte order for {@link #read()}
     * @param bits {@link BitOutputStream} size
     * @param bitOrder byte order for {@link BitOutputStream}
     */
    public AdpcmInputStream(InputStream in, ByteOrder byteOrder, int bits, ByteOrder bitOrder) {
        super(new BitInputStream(in, bits, bitOrder));
        this.byteOrder = byteOrder;
        this.decoder = getCodec();
//Debug.println(this.in);
    }

    /** ADPCM (4bit) length */
    @Override
    public int available() throws IOException {
//Debug.println("0: " + in.available() + ", " + ((in.available() * 2) + (rest ? 1 : 0)));
        // TODO "* 2" calc should be in bits?
        return (in.available() * 2) + (rest ? 1 : 0);
    }

    /** remaining or not */
    protected boolean rest = false;
    /** current stream value */
    protected int current;

    /**
     * @return PCM H or L (8bit LSB available)
     */
    @Override
    public int read() throws IOException {
//Debug.println(in);
        if (!rest) {
            int adpcm = in.read();
//System.err.println("0: " + StringUtil.toHex2(adpcm));
            if (adpcm == -1) {
                return -1;
            }

            current = decoder.decode(adpcm);

            rest = true;
//System.err.println("1: " + StringUtil.toHex2(current & 0xff));
            if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                return (current & 0xff00) >> 8;
            } else {
                return current & 0xff;
            }
        } else {
            rest = false;
//System.err.println("2: " + StringUtil.toHex2((current & 0xff00) >> 8));
            if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                return current & 0xff;
            } else {
                return (current & 0xff00) >> 8;
            }
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                 ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte) c;

        int i = 1;
        try {
            for (; i < len ; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                if (b != null) {
                    b[off + i] = (byte) c;
                }
            }
        } catch (IOException e) {
            Debug.printStackTrace(e);
        }
        return i;
    }
}
