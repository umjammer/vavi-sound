/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;


/**
 * LongMessage.
 * <p>
 * Represents MFi specs. "Extended Status A".
 * </p>
 * <li>not in javax.sound.midi package.(MFi original)
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070116 nsano initial version <br>
 */
public abstract class LongMessage extends MfiMessage {

    protected LongMessage() {
        super(null);
    }

    /**
     *
     * @param delta
     * @param status
     * @param data1 extended status number
     * @param data2 function number
     */
    public LongMessage init(int delta, int status, int data1, byte[] data2) {
        data = new byte[3 + data2.length];
        length = data.length;

        data[0] = (byte) (delta & 0xff);
        data[1] = (byte) (status & 0xff);
        data[2] = (byte) (data1 & 0xff);    // extended status
        System.arraycopy(data2, 0, this.data, 3, data2.length);

        return this;
    }

    /** extended status */
    public int getCommand() {
        return data[2] & 0xff;
    }

    /**
     * data (0 ~ 2 no header)
     * @return copied data
     */
    public byte[] getData() {
        byte[] tmp = new byte[this.length - 3];
        System.arraycopy(this.data, 3, tmp, 0, this.length - 3);
        return tmp;
    }
}
