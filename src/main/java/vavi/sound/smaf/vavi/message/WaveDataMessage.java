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
import java.util.Arrays;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import vavi.sound.mfi.vavi.sequencer.YamahaMfiExclusive;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.MobileExclusive;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.vavi.sequencer.WaveSequencer;

import static java.lang.System.getLogger;
import static vavi.sound.mobile.MobileExclusive.packedSystex;
import static vavi.sound.mobile.MobileExclusive.wave;


/**
 * WaveDataMessage.
 * <p>
 * system property
 * <li>{@code vavi.sound.mobile.AudioEngine.disabled} ... not to use vavi.sound.mobile.AudioEngine but
 * to send the wave to the synthesizer as an exclusive, {@link MobileExclusive#wave} or, for a wave table
 * one, {@code 43 05 00}, default {@code false}</li>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public class WaveDataMessage extends SmafMessage
    implements WaveSequencer, MidiConvertible, Serializable {

    private static final Logger logger = getLogger(WaveDataMessage.class.getName());

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
    public WaveDataMessage init(int number, int format, byte[] data, int samplingRate, int samplingBits, int channels) {
        this.number = number;
        this.format = format;
        this.data = data;
        this.samplingRate = samplingRate;
        this.samplingBits = samplingBits;
        this.channels = channels;

        return this;
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

        if (waveTable) {
            // the "EXWV" exclusive as it is in the file, a wave table voice ("EXVO") plays it
            sysexMessage = packedSystex(YamahaMfiExclusive.wave(number, data));
        } else {
            sysexMessage = packedSystex(wave(SMAF_SYSEX_FUNCTION_ID_WAVE, number, format, channels, samplingBits, samplingRate, data));
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

    /**
     * @param data 10 id fm ch bt sr adpcm ...
     * @throws IllegalArgumentException when audio engine does not found
     * @see MobileExclusive#wave
     */
    @Override
    public void sequence(byte[] data, Receiver receiver) throws InvalidSmafDataException {
        assert data[0] == 0x10 : "illegal command";
        int id = data[1] & 0x7f;
        int format = data[2] & 0xff;

        int samplingRate = (data[5] & 0xff) * 0x100 + (data[6] & 0xff);
        int samplingBits = data[4];
        int channels = data[3];
        byte[] adpcm = Arrays.copyOfRange(data, 7, data.length - 1);

logger.log(Level.DEBUG, "WAVE DATA[" + id + "]: format=" + format + " samplingRate=" + samplingRate + " samplingBits=" + samplingBits + " channels=" + channels);
//try {
// java.nio.file.Files.write(Path.of("out.pcm"), data);
// logger.log(Level.DEBUG, "WAVE DATA saved to out.pcm");
//} catch (java.io.IOException e) {
// logger.log(Level.ERROR, e.getMessage(), e);
//}
        AudioEngine engine = AudioEngineFactory.getAudioEngine(format);
        engine.setData(id, -1, samplingRate, samplingBits, channels, adpcm, false);
    }
}
