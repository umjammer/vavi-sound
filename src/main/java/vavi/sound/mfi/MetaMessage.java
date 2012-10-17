/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;


/**
 * MetaMessage.
 * <p>
 * 拡張情報に準拠。
 * </p>
 * <pre>
 * this class {@link #data} in [MFi meta]
 * +--+--+--+--+--+--+--+--+--+--+--+-
 * |00 ff fd|LL LL|TT|DD DD ...
 * +--+--+--+--+--+--+--+--+--+--+--+-
 * length (LL) = 1 (TT) + data length (DD ...)
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020629 nsano initial version <br>
 *          0.01 030921 nsano refine <br>
 */
public class MetaMessage extends MfiMessage {

    /** = delta (1) + status (1) + data1 (1) + length (2) + type (1) */
    protected static final int HEADER_LENGTH = 6;

    /** */
    public MetaMessage() {
        super(new byte[0]);
    }

    /** */
    protected MetaMessage(byte[] message) {
        super(message);
    }

    /**
     * <p>
     * {@link javax.sound.midi.MetaMessage} compatible. 
     * </p>
     */
    public void setMessage(int type, byte[] data, int length)
        throws InvalidMfiDataException {

        byte[] tmp = new byte[HEADER_LENGTH + length];

        tmp[0] = (byte) 0x00; // delta time always 0
        tmp[1] = (byte) 0xff; // normal
        tmp[2] = (byte) 0xfd; // 拡張情報空番 0xfd を勝手に(TODO) Meta とする
        tmp[3] = (byte) (((length + 1) / 0x100) & 0xff);
        tmp[4] = (byte) (((length + 1) % 0x100) & 0xff);
        tmp[5] = (byte) type;
        System.arraycopy(data, 0, tmp, HEADER_LENGTH, length);

        super.setMessage(tmp, tmp.length);
    }

    /**
     * Meta 番号
     * <p>
     * {@link javax.sound.midi.MetaMessage} compatible. 
     * </p>
     */
    public int getType() {
        return data[5] & 0xff;
    }

    /**
     * data (ヘッダ、先頭の Meta type は無し)
     * <p>
     * {@link javax.sound.midi.MetaMessage} compatible.
     * </p>
     * @return コピー
     */
    public byte[] getData() {
        byte[] tmp = new byte[this.length - HEADER_LENGTH];
        System.arraycopy(this.data, HEADER_LENGTH, tmp, 0, this.length - HEADER_LENGTH);
        return tmp;
    }
}

/* */
