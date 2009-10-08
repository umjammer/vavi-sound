/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.ChannelMessage;
import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;


/**
 * ChangeBankMessage.
 * <pre>
 *  0xff, 0xe# âπåπêßå‰èÓïÒ
 *  channel    true
 *  delta    ?
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.11 030821 nsano implements {@link MidiConvertible} <br>
 *          0.12 030920 nsano repackage <br>
 *          0.13 031203 nsano implements {@link ChannelMessage} <br>
 */
public class ChangeBankMessage extends ShortMessage
    implements ChannelMessage, MidiConvertible {

    /** */
    private int voice;
    /** GM the 6 bit */
    private int bank;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 0xe1
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- bank
     *  +- voice
     * </pre>
     */
    public ChangeBankMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xe1, data2);

        this.voice = (data2 & 0xc0) >> 6;
        this.bank  =  data2 & 0x3f;
    }

    /** for {@link MidiConvertible} */
    public ChangeBankMessage() {
        super(0, 0xff, 0xe1, 0);
    }

    /** */
    public int getBank() {
        return bank;
    }

    /** */
    public void setBank(int bank) {
        this.bank = bank & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0xc0) | this.bank);
    }

    /** */
    public int getVoice() {
        return voice;
    }

    /** */
    public void setVoice(int voice) {
        this.voice = voice & 0x03;
        this.data[3] = (byte) ((this.data[3] & 0x3f) | (this.voice << 6));
    }

    /** */
    public String toString() {
        return "ChangeBank:" +
               " voice=" + voice +
               " bank="  + bank;
    }

    //----

    /** */
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int channel = getVoice() + 4 * context.getMfiTrackNumber();
//Debug.println("track: "+context.getTrackNumber()+", voice: "+getVoice());

//Debug.println("bank[" + channel + "]: " + getBank());
        channel = context.setBank(channel, getBank());

        return null;
    }
}

/* */
