/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;


/**
 * LongMessage.
 * <p>
 * MFi 仕様の"拡張ステータス A"を表します。
 * </p>
 * <li>javax.sound.midi パッケージにはない。(MFi オリジナル)
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070116 nsano initial version <br>
 */
public abstract class LongMessage extends MfiMessage {

    /**
     *
     * @param delta
     * @param status
     * @param data1 拡張ステータス番号
     * @param data2 機能の値
     */
    public LongMessage(int delta, int status, int data1, byte[] data2) {
        super(new byte[3 + data2.length]);

        data[0] = (byte) (delta & 0xff);
        data[1] = (byte) (status & 0xff);
        data[2] = (byte) (data1 & 0xff);    // extended status
        System.arraycopy(data2, 0, this.data, 3, data2.length);
    }

    /** extended status */
    public int getCommand() {
        return data[2] & 0xff;
    }

    /**
     * data (0 ~ 2 ヘッダ無し)
     * @return コピー
     */
    public byte[] getData() {
        byte[] tmp = new byte[this.length - 3];
        System.arraycopy(this.data, 3, tmp, 0, this.length - 3);
        return tmp;
    }
}

/* */
