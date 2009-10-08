/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import java.io.InputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmInputStream;
import vavi.sound.adpcm.Codec;


/**
 * G721 InputStream.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030714 nsano initial version <br>
 *          0.01 030714 nsano fine tune <br>
 *          0.02 030714 nsano fix available() <br>
 *          0.03 030715 nsano read() endian �Ή� <br>
 *          0.10 060427 nsano refactoring <br>
 */
public class G721InputStream extends AdpcmInputStream {

    /** �f�R�[�_ */
    protected Codec getCodec() {
        return new G721();
    }

    /**
     * {@link vavi.io.BitInputStream} �� 4bit little endian �Œ�
     * <li>TODO BitInputStream �� endian
     * <li>TODO PCM encoding
     * @param in CCITT ADPCM
     * @param byteOrder #read() ���̃o�C�g�I�[�_
     */
    public G721InputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 4, ByteOrder.LITTLE_ENDIAN);
        ((G721) decoder).setEncoding(encoding);
    }
}

/* */
