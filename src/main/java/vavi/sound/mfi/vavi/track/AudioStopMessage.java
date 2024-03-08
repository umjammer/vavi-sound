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
import vavi.sound.mfi.LongMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mfi.vavi.sequencer.MfiMessageStore;
import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.mobile.AudioEngine;


/**
 * AudioStopMessage.
 * <pre>
 *  0x7f, 0x01
 *  channel true
 *  delta   ?
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070117 nsano initial version <br>
 */
public class AudioStopMessage extends LongMessage
    implements ChannelMessage, MidiConvertible, AudioDataSequencer {

    /** */
    private int voice;
    /** */
    private int index;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status should be 0x7f
     * @param data1 should be 0x01
     * @param data2 1 byte
     */
    public AudioStopMessage(int delta, int status, int data1, byte[] data2) {
        super(delta, 0x7f, 0x01, data2);

        this.voice = (data2[0] & 0xc0) >> 6;
        this.index =  data2[0] & 0x3f;
    }

    /* */
    @Override
    public int getVoice() {
        return voice;
    }

    /** */
    public int getIndex() {
        return index;
    }

    /* */
    @Override
    public void setVoice(int voice) {
        this.voice = voice & 0x03;
        this.data[3] = (byte) ((this.data[3] & 0x3f) | (this.voice << 6));
    }

    /** */
    public String toString() {
        return "AudioStop:" +
        " voice=" + voice +
        " index=" + index;
    }

    //----

    /**
     * @throws InvalidMidiDataException
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {

        MetaMessage metaMessage = new MetaMessage();

        int id = MfiMessageStore.put(this);
        byte[] data = {
            VaviMidiDeviceProvider.MANUFACTURER_ID,
            META_FUNCTION_ID_MFi4,
            (byte) ((id / 0x100) & 0xff),
            (byte) ((id % 0x100) & 0xff)
        };
        metaMessage.setMessage(0x7f,    // sequencer specific meta event
                               data,
                               data.length);

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrent())
        };
    }

    /** @throws IllegalArgumentException when audio engine does not found */
    @Override
    public void sequence() throws InvalidMfiDataException {
        int id = getIndex();

        AudioEngine engine = Factory.getAudioEngine();
        engine.stop(id);
    }
}
