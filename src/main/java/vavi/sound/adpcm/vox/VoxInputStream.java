/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.vox;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.AdpcmInputStream;
import vavi.sound.adpcm.Codec;


/**
 * Vox InputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030816 nsano initial version <br>
 */
public class VoxInputStream extends AdpcmInputStream {

    @Override
    protected Codec getCodec() {
        return new Vox();
    }

    /**
     * {@link vavi.io.BitInputStream} is 4bit big endian fixed
     * TODO vox is big endian ?
     */
    public VoxInputStream(InputStream in, ByteOrder byteOrder) {
        super(in, byteOrder, 4, ByteOrder.BIG_ENDIAN);
    }

    @Override
    public int available() throws IOException {
//logger.log(Level.TRACE, "0: " + in.available() + ", " + ((in.available() * 2) + (rest ? 1 : 0)));
        return (in.available() * 2) + (rest ? 1 : 0);
    }

    /**
     * TODO endian
     */
    @Override
    public int read() throws IOException {
//logger.log(Level.TRACE, in);
        if (!rest) {
            int adpcm = in.read();
            if (adpcm == -1) {
                return -1;
            }

            current = decoder.decode(adpcm) * 16; // TODO check!!!

            rest = true;
//logger.log(Level.TRACE, "1: " + StringUtil.toHex2(current & 0xff));
            if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                return (current & 0xff00) >> 8;
            } else {
                return current & 0xff;
            }
        } else {
            rest = false;
//logger.log(Level.TRACE, "2: " + StringUtil.toHex2((current & 0xff00) >> 8));
            if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                return current & 0xff;
            } else {
                return (current & 0xff00) >> 8;
            }
        }
    }
}
