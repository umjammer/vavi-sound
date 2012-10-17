/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.ChannelMessage;
import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mfi.vavi.sequencer.MfiMessageStore;
import vavi.sound.midi.VaviMidiDeviceProvider;


/**
 * AudioChannelPanpotMessage.
 * <pre>
 *  0x7f, 0x81
 *  channel true
 *  delta   ?
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 070117 nsano initial version <br>
 * @since MFi4
 */
public class AudioChannelPanpotMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, AudioDataSequencer {

    /** */
    private int voice;
    /** left 0, 1 - center 32 - right 63 */
    private int panpot = 32;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status should be 0x7f
     * @param data1 should be 0x81
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- panpot
     *  +- voice
     * </pre>
     */
    public AudioChannelPanpotMessage(int delta, int status, int data1, int data2) {
        super(delta, 0x7f, 0x81, data2);

        this.voice  = (data2 & 0xc0) >> 6;
        this.panpot =  data2 & 0x3f;
    }

    /** */
    public int getPanpot() {
        return panpot;
    }

    /** */
    public void setPanpot(int panpot) {
        this.panpot = panpot & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0xc0) | this.panpot);
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
        return "AudioChannelPanpot:" +
            " voice="  + voice +
            " panpot=" + panpot;
    }

    //----

    /** */
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        MetaMessage metaMessage = new MetaMessage();

        int id = MfiMessageStore.put(this);
        byte[] data = {
            VaviMidiDeviceProvider.MANUFACTURER_ID,
            META_FUNCTION_ID_MFi4,
            (byte) ((id / 0x100) & 0xff),
            (byte) ((id % 0x100) & 0xff)
        };
        metaMessage.setMessage(0x7f,    // シーケンサー固有メタイベント
                               data,
                               data.length);

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrent())
        };
    }

    /** */
    public void sequence() throws InvalidMfiDataException {
        // TODO Auto-generated method stub
    }
}

/* */
