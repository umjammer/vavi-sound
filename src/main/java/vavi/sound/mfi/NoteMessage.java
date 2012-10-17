/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import vavi.util.StringUtil;


/**
 * 音符を表すメッセージです．
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.10 020627 nsano MFi2 compliant <br>
 *          0.11 020630 nsano refine <br>
 *          0.12 031126 nsano fix setNote <br>
 *          0.13 031128 nsano remove setDelta <br>
 *          0.14 031203 nsano implements {@link ChannelMessage} <br>
 */
public class NoteMessage extends MfiMessage implements ChannelMessage {

    /** 音階 0 ~ 0x3e */
    private int note;
    /** voice No. 0 ~ 3 */
    private int voice;
    /** 音長 (delta) */
    private int gateTime;
    /** 音強 0 ~ 63 */
    private int velocity;
    /**
     * オクターブシフト
     * <pre>
     * 01 up 1 octave
     * 00 no shift
     * 11 down 1 octave
     * 10 down 2 octave
     * </pre>
     */
    private int shift;

    /**
     * Creates a note message for MFi.
     *
     * @param delta delta time
     * @param status <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- note 0x00 ~ 0x3e
     *  +- voice no.
     * </pre>
     * @param data    note length
     */
    public NoteMessage(int delta, int status, int data) {
        super(new byte[] {
            (byte) (delta  & 0xff),
            (byte) (status & 0xff),
            (byte) (data   & 0xff) });

        this.note     =  status & 0x3f;
        this.voice    = (status & 0xc0) >> 6;
        this.gateTime =  data;
        this.velocity =  63;
        this.shift    =  0;
    }

    /**
     * Creates a note message for MFi2.
     * note = 1 の場合
     *
     * @param delta delta time
     * @param status see below
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +------ note 0x00 ~ 0x3e
     *  +--------- voice no.
     * </pre>
     * @param data1 note length
     * @param data2 see below
     * <pre>
     *  765432 10
     *  ~~~~~~ ~~
     *  |      +-- shift
     *  +--------- velocity
     * </pre>
     */
    public NoteMessage(int delta, int status, int data1, int data2) {
        super(new byte[] {
            (byte) (delta  & 0xff),
            (byte) (status & 0xff),
            (byte) (data1  & 0xff),
            (byte) (data2  & 0xff) });

        this.note     =  status & 0x3f;
        this.voice    = (status & 0xc0) >> 6;
        this.gateTime =  data1;
        this.velocity = (data2 & 0xfc) >> 2;
        this.shift    =  data2 & 0x03;
    }

    /** note = 1 */
    protected NoteMessage() {
        super(new byte[4]);
    }

    /**
     * 音階を取得します．
     * @return 音階
     */
    public int getNote() {
        switch (shift) {
        case 1: // 01
            return note + 12;
        case 0: // 00
            return note;
        case 3: // 11
            return note - 12;
        case 2: // 10
            return note - 24;
        default:
            throw new IllegalArgumentException("shift: " + shift);
        }
    }

    /**
     * 音階を設定します．
     * @param note MFi の音階
     */
    public void setNote(int note) {
        if (note >= 0x3f) {
            this.shift = 1;
            this.note = note - 12;
            this.data[3] = (byte) ((this.data[3] & 0xfc) | 0x01);
        } else if (note < -12) {
            this.shift = 2;
            this.note = note + 24;
            this.data[3] = (byte) ((this.data[3] & 0xfc) | 0x02);
        } else if (note < 0) {
            this.shift = 3;
            this.note = note + 12;
            this.data[3] = (byte) ((this.data[3] & 0xfc) | 0x03);
        } else {
            this.shift = 0;
            this.note = note;
        }
        this.data[1] = (byte) ((this.data[1] & 0xc0) | (this.note & 0x3f));
    }

    /**
     * ボイスナンバを取得します．
     * @return ボイスナンバ
     */
    public int getVoice() {
        return voice;
    }

    /**
     * ボイスナンバを設定します．
     * @param voice ボイスナンバ
     */
    public void setVoice(int voice) {
        this.voice = voice & 0x03;
        this.data[1] = (byte) ((this.data[1] & 0x3f) | (this.voice << 6));
    }

    /**
     * 音長を取得します．
     * @return 音長
     */
    public int getGateTime() {
        return gateTime;
    }

    /**
     * 音長を設定します．
     * @param gateTime 音長
     */
    public void setGateTime(int gateTime) {
        this.gateTime = gateTime;
        this.data[2] = (byte) gateTime;
    }

    /**
     * 音強を取得します．
     * @return 音強
     */
    public int getVelocity() {
        return velocity;
    }

    /**
     * 音強を設定します．
     * @param velocity 音強
     */
    public void setVelocity(int velocity) {
        this.velocity = velocity & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0x03) | (this.velocity << 2));
    }

    /** */
    public String toString() {
        return "Note:" +
            " delta="    + StringUtil.toHex2(data[0])  +
            " voice="    + StringUtil.toHex2(voice)    +
            " note="     + StringUtil.toHex2(note)     +
            " gateTime=" + StringUtil.toHex2(gateTime) +
            " velocity=" + StringUtil.toHex2(velocity);
    }
}

/* */
