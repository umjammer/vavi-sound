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


/**
 * AdpcmInputStream.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030714 nsano initial version <br>
 *          0.01 030714 nsano fine tune <br>
 *          0.02 030714 nsano fix available() <br>
 *          0.03 030715 nsano read() endian 対応 <br>
 *          0.10 060427 nsano refactoring <br>
 */
public abstract class AdpcmInputStream extends FilterInputStream {

    /** #read() が返す PCM のフォーマット */
    protected AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;

    /** #read() が返す PCM のバイトオーダ */
    protected ByteOrder byteOrder;

    /** デコーダ */
    protected Codec decoder;

    /** */
    protected abstract Codec getCodec();

    /**
     * @param in PCM
     * @param byteOrder {@link #read()} 時のバイトオーダ 
     * @param bits {@link BitOutputStream} のサイズ
     * @param bitOrder {@link BitOutputStream} のバイトオーダ 
     */
    public AdpcmInputStream(InputStream in, ByteOrder byteOrder, int bits, ByteOrder bitOrder) {
        super(new BitInputStream(in, bits, bitOrder));
        this.byteOrder = byteOrder;
        this.decoder = getCodec();
//Debug.println(this.in);
    }

    /** ADPCM (4bit) 換算時の長さ */
    public int available() throws IOException {
//Debug.println("0: " + in.available() + ", " + ((in.available() * 2) + (rest ? 1 : 0)));
        // TODO * 2 とか bits で計算すべき？
        return (in.available() * 2) + (rest ? 1 : 0);
    }

    /** 残っているかどうか */
    protected boolean rest = false;
    /** 現在の値 */
    protected int current;

    /**
     * @return PCM H or L (8bit LSB 有効)
     */
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

    /* */
    public int read(byte b[], int off, int len) throws IOException {
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
e.printStackTrace(System.err);
        }
        return i;
    }
}

/* */
