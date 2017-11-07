/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.IOException;
import java.io.OutputStream;


/**
 * SMAF メッセージの基底クラスです．
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public abstract class SmafMessage {

    /** */
    protected int duration;

    /**
     * @param duration
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * @return duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * @return コピー
     */
    public abstract byte[] getMessage();

    /** */
    public abstract int getLength();

    /** */
    protected void writeOneToTwo(OutputStream os, int value) throws IOException {
        if (value < 128) {
            os.write(value);
        } else {
            os.write(0x80 | (value - 0x80) / 0x80);
            os.write((value - 0x80) % 0x80);
        }
    }

    /** */
    protected void writeOneToFour(OutputStream os, int value) throws IOException {
        // TODO
    }
}

/* */
