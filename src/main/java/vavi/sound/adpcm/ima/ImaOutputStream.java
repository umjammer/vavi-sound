/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ima;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.io.InputEngineOutputStream;


/**
 * IMA OutputStream.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060202 nsano initial version <br>
 */
public class ImaOutputStream extends FilterOutputStream {

    /**
     * バイトオーダーは little endian
     */
    public ImaOutputStream(OutputStream out,
                           int samplesPerBlock,
                           int channels)
        throws IOException {

        this(out, samplesPerBlock, channels, ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * @param samplesPerBlock use 505 bytes as default
     * @param channels use 1 as default
     */
    public ImaOutputStream(OutputStream out,
                           int samplesPerBlock,
                           int channels,
                           ByteOrder byteOrder)
        throws IOException {

        super(new InputEngineOutputStream(new ImaInputEngine(out, samplesPerBlock, channels, byteOrder), samplesPerBlock * 2));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }
}

/* */
