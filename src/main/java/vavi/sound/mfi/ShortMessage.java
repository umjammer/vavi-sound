/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;


/**
 * ShortMessage を表すクラスです。
 * <p>
 * MFi 仕様の"拡張ステータス B"を表します。
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 020627 nsano refine <br>
 *          0.11 030920 nsano add {@link #getCommand()} <br>
 *          0.12 030920 nsano add constants <br>
 */
public abstract class ShortMessage extends MfiMessage {

    /** @see #getCommand() */
    public static final int NON_CHANNEL = 0xb0;

    /** @see #getCommand() */
    public static final int TEMPO = 0xc0;

    /** @see #getCommand() */
    public static final int PLAY_CONTROL = 0xd0;

    /** @see #getCommand() */
    public static final int SOUND_CONTROL = 0xe0;

    /**
     *
     * @param delta
     * @param status
     * @param data1 拡張ステータス番号
     * @param data2 機能の値
     */
    public ShortMessage(int delta, int status, int data1, int data2) {
        super(new byte[] {
                (byte) (delta & 0xff),
                (byte) (status & 0xff),
                (byte) (data1 & 0xff),
                (byte) (data2 & 0xff)
            });
    }

    /** extended status */
    public int getCommand() {
        return data[2] & 0xff;
    }

    /** data */
    public int getData() {
        return data[3] & 0xff;
    }
}

/* */
