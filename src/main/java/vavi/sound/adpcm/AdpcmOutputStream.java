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
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030827 nsano initial version <br>
 *          0.10 060427 nsano refactoring <br>
 */
public abstract class AdpcmOutputStream extends FilterOutputStream {

    /** #write(int) に渡す PCM のフォーマット */
    protected AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;

    /** #write(int) に渡す PCM のバイトオーダ */
    protected ByteOrder byteOrder;

    /** エンコーダ */
    protected Codec encoder;

    /** */
    protected abstract Codec getCodec();

    /**
     * TODO BitOutputStream の引数をまとめて BitOutputStream にする？
     * @param out ADPCM
     * @param byteOrder {@link #write(int)} のバイトオーダ 
     * @param bits {@link BitOutputStream} のサイズ
     * @param bitOrder {@link BitOutputStream} のバイトオーダ 
     */
    public AdpcmOutputStream(OutputStream out, ByteOrder byteOrder, int bits, ByteOrder bitOrder) {
        super(new BitOutputStream(out, bits, bitOrder));
        this.byteOrder = byteOrder;
        this.encoder = getCodec();
//Debug.println(this.out);
    }

    /** 残っていないかどうか (PCM L or H 片方保持してるかどうか) */
    protected boolean flushed = true;
    /** 現在の値 (PCM L or H 片方しか来てない時の保持用) */
    protected int current;

    /**
     * @param b PCM H or L byte を {@link #byteOrder} 順に指定 (LSB 8bit 有効)
     */
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

/* */
