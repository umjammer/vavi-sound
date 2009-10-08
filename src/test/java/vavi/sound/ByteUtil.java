/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound;

import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;


/**
 * ByteUtil. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060203 nsano initial version <br>
 */
public class ByteUtil {

    /** */
    private AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;

    /** */
    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(AudioFormat.Encoding encoding) {
        this.encoding = encoding;
    }

    /**
     * @param byteOrder the byteOrder to set
     */
    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    /** */
    public final int readAsInt(byte[] buffer, int index) {
        int result = 0;
        if (byteOrder.equals(ByteOrder.LITTLE_ENDIAN)) {
            result = (buffer[index] & 0xff) | ((buffer[index + 1] & 0xff) << 8);
        } else {
            result = (buffer[index + 1] & 0xff) | ((buffer[index] & 0xff) << 8);
        }
        if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
            if ((result & 0x8000) != 0) {
                result -= 0x10000;
            }
        }
        return result;
    }

    /** */
    public final void writeAsByteArray(byte[] buffer, int index, int value) {
        if (byteOrder.equals(ByteOrder.LITTLE_ENDIAN)) {
            buffer[index    ] = (byte)  (value & 0x00ff);
            buffer[index + 1] = (byte) ((value & 0xff00) >> 8);
        } else {
            buffer[index    ] = (byte) ((value & 0xff00) >> 8);
            buffer[index + 1] = (byte)  (value & 0x00ff);
        }
    }
}

/* */
