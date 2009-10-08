/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ima;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.io.LittleEndianDataInputStream;
import vavi.util.Debug;


/**
 * IMA OutputStream.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060202 nsano initial version <br>
 */
public class ImaOutputStream extends FilterOutputStream {

    /** */
    private ByteOrder byteOrder;

    /**
     * バイトオーダーは little endian
     */
    public ImaOutputStream(OutputStream out)
        throws IOException {

        this(out, ByteOrder.LITTLE_ENDIAN);
    }

    /** */
    private OutputStream realOut;

    /**
     */
    public ImaOutputStream(OutputStream out, final ByteOrder byteOrder)
        throws IOException {

        super(new ByteArrayOutputStream());

        this.byteOrder = byteOrder;
Debug.println("byteOrder: " + this.byteOrder);

        realOut = out;
    }

    /**
     * 必ず呼んでね。
     */
    public void close() throws IOException {

        final Ima encoder = new Ima();

        try {
            LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(new ByteArrayInputStream(((ByteArrayOutputStream) out).toByteArray()));
            int length = ledis.available();
Debug.println("length: " + length);
            byte[] adpcm = new byte[length / 4];
            int[] pcm = new int[length / 2];
            for (int i = 0; i < pcm.length; i++) {
                pcm[i] = ledis.readShort();
            }
            encoder.encodeBlock(1, pcm, pcm.length, new int[88], adpcm, 1);

            realOut.write(adpcm);

        } catch (IOException e) {
Debug.printStackTrace(e);
        } finally {
            try {
                realOut.flush();
                realOut.close();
            } catch (IOException e) {
Debug.println(e);
            }
        }

        realOut.close();
    }
}

/* */
