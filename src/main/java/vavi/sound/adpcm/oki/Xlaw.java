/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.oki;

import javax.sound.sampled.AudioFormat;

import vavi.sound.adpcm.Codec;


/**
 * x-law codec base class.
 * <p>
 * TODO ccitt G.711 なので統合する
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public abstract class Xlaw implements Codec {

    /** */
    protected AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;

    /** signed or unsigned */
    public void setEncoding(AudioFormat.Encoding encoding) {
        this.encoding = encoding;
    }

    /** 8 or 16 */
    protected int bit;

    /** */
    public void setBit(int bit) {
        this.bit = bit;
    }

    /**
     */
    protected int[][] decodeTable;

    /** */
    protected int[] encodeTable;

    /** */
    public int decode(int xlaw) {
        if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
            if (bit == 8) {
                return decodeTable[xlaw][0] ^ 0x80;
            } else if (bit == 16) { // TODO endian check
                return ((decodeTable[xlaw][0] << 8) |
                         decodeTable[xlaw][1]      ) ^ 0x8000;
            } else {
                throw new IllegalArgumentException("illegal bit: " + bit);
            }
        } else if (AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)) {
            if (bit == 8) {
                return decodeTable[xlaw][0];
            } else if (bit == 16) { // TODO endian check
                return ((decodeTable[xlaw][0] << 8) |
                         decodeTable[xlaw][1]      );
            } else {
                throw new IllegalArgumentException("illegal bit: " + bit);
            }
        } else {
            throw new IllegalArgumentException(encoding.toString());
        }
    }

    /** */
    public int encode(int linear) {
        if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
            if (bit == 8) {
                return encodeTable[linear ^ 0x80];
            } else if (bit == 16) {
                return encodeTable[(linear >> 8) ^ 0x80]; // TODO
            } else {
                throw new IllegalArgumentException("illegal bit: " + bit);
            }
        } else if (AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)) {
            if (bit == 8) {
                return encodeTable[linear];
            } else if (bit == 16) {
                return  encodeTable[linear >> 8]; // TODO
            } else {
                throw new IllegalArgumentException("illegal bit: " + bit);
            }
        } else {
            throw new IllegalArgumentException(encoding.toString());
        }
    }
}

/* */
