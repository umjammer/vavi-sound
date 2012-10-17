/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import java.io.Serializable;


/**
 * Mfi メッセージの基底クラスです．
 * <pre>
 * +--+--+--+--+--+--+--+--+-
 * |Δ|ST|D1 D2 ...
 * +--+--+--+--+--+--+--+--+-
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.01 020627 nsano refine <br>
 *          0.02 030820 nsano implements {@link Serializable} <br>
 *          0.03 030820 nsano {@link javax.sound.midi.MidiMessage} compatible <br>
 *          0.04 031128 nsano move setDelta to here <br>
 */
public abstract class MfiMessage
    implements Cloneable, Serializable {

    /** 0xff */
    public static final int STATUS_NORMAL = 0xff;

    /** 0xbf */
    public static final int STATUS_CLASS_C = 0xbf;

    /** 0x7f */
    public static final int STATUS_CLASS_B = 0x7f;

    /** 0x3f */
    public static final int STATUS_CLASS_A = 0x3f;

    /**
     * <pre>
     * +--+--+--+--+--+--+--+--+-
     * |Δ|ST|D1 D2 ...
     * +--+--+--+--+--+--+--+--+-
     * </pre>
     */
    protected byte[] data;

    /** */
    protected int length;

    /**
     * @after {@link #length} が設定される 
     */
    protected MfiMessage(byte[] data) {
        this.data = data;
        if (data != null) {
            this.length = data.length;
        }
    }

    /**
     * メッセージのバイト配列を設定します．
     */
    protected void setMessage(byte[] data, int length)
        throws InvalidMfiDataException {

        if (length < 0 || (length > 0 && length > data.length)) {
          throw new IndexOutOfBoundsException("length out of bounds: "+length);
        }
        this.length = length;

        if (this.data == null || this.data.length < this.length) {
            this.data = new byte[this.length];
        }
        System.arraycopy(data, 0, this.data, 0, length);
    }

    /**
     * メッセージのバイト配列を取得します．
     * @return コピー
     */
    public byte[] getMessage() {
        byte[] returnedArray = new byte[length];
        System.arraycopy(data, 0, returnedArray, 0, length);
        return returnedArray;
    }

    /** 前のデータが実行されてからの時間を取得します． */
    public int getDelta() {
        if (length > 0) {
            return data[0] & 0xff;
        }
        return 0;  // TODO this.data が設定されていない場合
    }

    /**
     * delta を設定します．
     * @param delta delta
     */
    public void setDelta(int delta) {
        this.data[0] = (byte) (delta & 0xff);
    }

    /** ステータス情報を取得します． */
    public int getStatus() {
        if (length > 1) {
            return data[1] & 0xff;
        }
        return 0;  // TODO this.data が設定されていない場合
    }

    /** メッセージの長さを取得します． */
    public int getLength() {
        return length;
    }
}

/* */
