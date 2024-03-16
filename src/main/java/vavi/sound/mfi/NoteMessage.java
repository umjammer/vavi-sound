/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

/**
 * Represent note message.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 020627 nsano MFi2 compliant <br>
 *          0.11 020630 nsano refine <br>
 *          0.12 031126 nsano fix setNote <br>
 *          0.13 031128 nsano remove setDelta <br>
 *          0.14 031203 nsano implements {@link ChannelMessage} <br>
 */
public class NoteMessage extends MfiMessage implements ChannelMessage {

    /** note 0 ~ 0x3e */
    private int note;
    /** voice No. 0 ~ 3 */
    private int voice;
    /** length (delta) */
    private int gateTime;
    /** velocity 0 ~ 63 */
    private int velocity;
    /**
     * Octave shift.
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
     * when note = 1.
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
     * Gets note.
     * @return note
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
     * Sets note.
     * @param note MFi note
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
     * Gets voice number.
     * @return voice number
     */
    @Override
    public int getVoice() {
        return voice;
    }

    /**
     * Sets voice number.
     * @param voice voice number
     */
    @Override
    public void setVoice(int voice) {
        this.voice = voice & 0x03;
        this.data[1] = (byte) ((this.data[1] & 0x3f) | (this.voice << 6));
    }

    /**
     * Gets note length.
     * @return note length
     */
    public int getGateTime() {
        return gateTime;
    }

    /**
     * Sets note length.
     * @param gateTime note length
     */
    public void setGateTime(int gateTime) {
        this.gateTime = gateTime;
        this.data[2] = (byte) gateTime;
    }

    /**
     * Gets note velocity.
     * @return note velocity
     */
    public int getVelocity() {
        return velocity;
    }

    /**
     * Sets note velocity.
     * @param velocity note velocity
     */
    public void setVelocity(int velocity) {
        this.velocity = velocity & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0x03) | (this.velocity << 2));
    }

    @Override
    public String toString() {
        return "Note:" +
            " delta="    + String.format("%02x", data[0])  +
            " voice="    + String.format("%02x", voice)    +
            " note="     + String.format("%02x", note)     +
            " gateTime=" + String.format("%02x", gateTime) +
            " velocity=" + String.format("%02x", velocity);
    }
}
