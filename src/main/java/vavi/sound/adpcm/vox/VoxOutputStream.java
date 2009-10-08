/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.vox;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;

import vavi.sound.adpcm.AdpcmOutputStream;
import vavi.sound.adpcm.Codec;


/**
 * VOX OutputStream
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 051110 nsano initial version <br>
 */
public class VoxOutputStream extends AdpcmOutputStream {

    /** エンコーダ */
    protected Codec getCodec() {
        return new Vox();
    }

    /**
     * {@link vavi.io.BitInputStream} は 4bit big endian 固定
     * TODO check endian
     * @param out ADPCM
     * @param byteOrder #write(int) のバイトオーダ 
     */
    public VoxOutputStream(OutputStream out, ByteOrder byteOrder) {
        super(out, byteOrder, 4, ByteOrder.BIG_ENDIAN);
    }

    /**
     * @param b 
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

// System.err.println("current: " + StringUtil.toHex4(current));
            out.write(encoder.encode(current / 16)); // BitOutputStream write 4bit

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
