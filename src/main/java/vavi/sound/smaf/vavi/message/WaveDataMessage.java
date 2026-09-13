/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message;

import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.SysexMessage;

import vavi.sound.mfi.vavi.sequencer.YamahaExclusive;
import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.StreamExclusive;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.vavi.sequencer.SmafMessageStore;
import vavi.sound.smaf.vavi.sequencer.WaveSequencer;

import static java.lang.System.getLogger;


/**
 * WaveDataMessage.
 * <p>
 * system property
 * <li>{@code vavi.sound.mobile.AudioEngine.disabled} ... not to use vavi.sound.mobile.AudioEngine but
 * to send the wave to the synthesizer as an exclusive, {@link StreamExclusive#wave} or, for a wave table
 * one, {@code 43 05 00}, default {@code false}</li>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public class WaveDataMessage extends SmafMessage
    implements WaveSequencer, MidiConvertible, Serializable {

    private static final Logger logger = getLogger(WaveDataMessage.class.getName());

    /** */
    private final int number;
    /** */
    private final int format;
    /** */
    private final byte[] data;
    /** */
    private final int samplingRate;
    /** */
    private final int samplingBits;
    /** */
    private final int channels;

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

    @Override
    public String toString() {
        return "WaveData:" +
            " id=" + number  +
            " format=" + format  +
            " samplingRate=" + samplingRate  +
            " samplingBits=" + samplingBits  +
            " channels="    + channels;
    }

    // ----

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

    @Override
    public int getLength() {
        return data.length;
    }

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        SysexMessage sysexMessage;

        if (!StreamExclusive.isEnabled()) {
            sysexMessage = new SysexMessage();
            int id = SmafMessageStore.put(this);
            byte[] data = {
                    VaviMidiDeviceProvider.MANUFACTURER_ID,
                    WaveSequencer.SYSEX_FUNCTION_ID_SMAF,
                    (byte) ((id / 0x100) & 0xff),
                    (byte) ((id % 0x100) & 0xff)
            };
            sysexMessage.setMessage(0xf0,    // sysex
                                    data,
                                    data.length);
        } else if (waveTable) {
            // the "EXWV" exclusive as it is in the file, a wave table voice ("EXVO") plays it
            sysexMessage = StreamExclusive.pack(YamahaExclusive.wave(number, data));
        } else {
            StreamExclusive.Format streamFormat = StreamExclusive.Format.valueOf(format);
            if (streamFormat == null) {
logger.log(Level.WARNING, "stream wave format not supported, skipped: " + this);
                return new MidiEvent[0];
            }
            sysexMessage = StreamExclusive.pack(StreamExclusive.wave(number, streamFormat, channels, samplingBits, samplingRate, data));
        }

        return new MidiEvent[] {
            new MidiEvent(sysexMessage, context.getCurrentTick())
        };
    }

    /** true: the wave of a wave table voice ("EXWV"), false: a stream one ("Mwa*", "Awa*") */
    private boolean waveTable;

    /** tells this is the wave of a wave table voice, an "EXWV" one, not a stream wave */
    public WaveDataMessage setWaveTable(boolean waveTable) {
        this.waveTable = waveTable;
        return this;
    }

    @Override
    public void sequence() throws InvalidSmafDataException {
logger.log(Level.DEBUG, "WAVE DATA[" + number + "]: " + this);
//try {
// java.nio.file.Files.write(Path.of("out.pcm"), data);
// logger.log(Level.DEBUG, "WAVE DATA saved to out.pcm");
//} catch (java.io.IOException e) {
// logger.log(Level.ERROR, e.getMessage(), e);
//}
        AudioEngine engine = Factory.getAudioEngine(format);
        engine.setData(number, -1, samplingRate, samplingBits, channels, data, false);
    }
}
