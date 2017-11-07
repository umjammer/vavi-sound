/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.io.Serializable;
import java.nio.ByteBuffer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.sequencer.SmafMessageStore;
import vavi.sound.smaf.sequencer.WaveSequencer;
import vavi.util.Debug;


/**
 * WaveDataMessage.
 * TODO SysexMessage とかじゃないの？
 * <pre>
 *
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public class WaveDataMessage extends SmafMessage
    implements WaveSequencer, MidiConvertible, Serializable {

    /** */
    private int number;
    /** */
    private int format;
    /** */
    private byte[] data;
    /** */
    private int samplingRate;
    /** */
    private int samplingBits;
    /** */
    private int channels;

    /**
     */
    public WaveDataMessage(int number, int format, byte[] data, int samplingRate, int samplingBits, int channels) {
        this.number = number;
        this.format = format;
        this.data = data;
        this.samplingRate = samplingRate;
        this.samplingBits = samplingBits;
        this.channels = channels;
    }

    /** */
    public String toString() {
        return "WaveData:" +
            " id=" + number  +
            " format=" + format  +
            " samplingRate=" + samplingRate  +
            " samplingBits=" + samplingBits  +
            " channels="    + channels;
    }

    //----

    /* */
    @Override
    public byte[] getMessage() {
        ByteBuffer bb = ByteBuffer.allocate(4 + 4 + data.length);
        bb.put((byte) 'A');
        bb.put((byte) 'w');
        bb.put((byte) 'a');
        bb.put((byte) number);
        bb.putInt(data.length);
        bb.put(data);
        return bb.array();
    }

    /* */
    @Override
    public int getLength() {
        return data.length;
    }

    /** */
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        MetaMessage metaMessage = new MetaMessage();

        int id = SmafMessageStore.put(this);
        byte[] data = {
            VaviMidiDeviceProvider.MANUFACTURER_ID,
            WaveSequencer.META_FUNCTION_ID_SMAF,
            (byte) ((id / 0x100) & 0xff),
            (byte) ((id % 0x100) & 0xff)
        };
        metaMessage.setMessage(0x7f,    // シーケンサー固有メタイベント
                               data,
                               data.length);

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrentTick())
        };
    }

    /* */
    public void sequence() throws InvalidSmafDataException {
Debug.println("WAVE DATA[" + number + "]: " + this);
//try {
// java.io.OutputStream os = new java.io.FileOutputStream("out.pcm");
// os.write(data);
// os.flush();
// os.close();
// Debug.println("WAVE DATA saved to out.pcm");
//} catch (java.io.IOException e) {
// Debug.printStackTrace(e);
//}
        AudioEngine engine = Factory.getAudioEngine(format);
        engine.setData(number, -1, samplingRate, samplingBits, channels, data, false);
    }
}

/* */
