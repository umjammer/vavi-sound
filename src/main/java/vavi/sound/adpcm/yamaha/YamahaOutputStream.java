/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.yamaha;

import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmOutputStream;
import vavi.sound.adpcm.Codec;


/**
 * YAMAHA MA# OutputStream
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050402 nsano initial version <br>
 */
public class YamahaOutputStream extends AdpcmOutputStream {

    /** �G���R�[�_ */
    protected Codec getCodec() {
        return new Yamaha();
    }

    /**
     * 
     * {@link vavi.io.BitOutputStream} �� 4bit little endian �Œ�
     * @param out ADPCM
     * @param byteOrder {@link #write(int)} �̃o�C�g�I�[�_ 
     */
    public YamahaOutputStream(OutputStream out, ByteOrder byteOrder) {
        super(out, byteOrder, 4, ByteOrder.LITTLE_ENDIAN);
    }
}

/* */
