/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.MidiEvent;
import vavi.sound.mfi.ChannelMessage;
import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.util.Debug;


/**
 * ChannelChangeMessage.
 * <pre>
 *  0xff, 0xe# 音源制御情報
 *  channel true
 *  delta   ?
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 030821 nsano implements {@link MidiConvertible} <br>
 *          0.02 030920 nsano repackage <br>
 *          0.03 031203 nsano implements {@link ChannelMessage} <br>
 * @since MFi2
 */
public class ChannelChangeMessage extends ShortMessage
    implements ChannelMessage, MidiConvertible {

    /** */
    private int voice;
    /** 0 - 15 ch */
    private int channel;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 0xe5
     * @param data2
     * <pre>
     *  76--3210
     *  ~~  ~~~~
     *  |   +- channel
     *  +- voice
     * </pre>
     */
    public ChannelChangeMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xe5, data2);

        this.voice   = (data2 & 0xc0) >> 6;
        this.channel =  data2 & 0x0f;
    }

    /** 0 - 15 ch */
    public int getChannel() {
        return channel;
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
        return "ChannelChange:" +
            " voice="   + voice +
            " channel=" + channel;
    }

    //----

    /** */
    public MidiEvent[] getMidiEvents(MidiContext context) {
Debug.println("ignore: " + this);
        return null;
    }
}

/* */
