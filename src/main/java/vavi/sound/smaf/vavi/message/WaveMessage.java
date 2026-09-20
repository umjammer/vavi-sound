/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import vavi.sound.midi.MidiUtil;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.MobileExclusive;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.vavi.VaviSmafSynthesizer.VaviSmafReceiver;
import vavi.sound.smaf.vavi.chunk.TrackChunk.FormatType;
import vavi.sound.smaf.vavi.sequencer.WaveSequencer;

import static java.lang.System.getLogger;
import static vavi.sound.mobile.MobileExclusive.off;
import static vavi.sound.mobile.MobileExclusive.on;
import static vavi.sound.mobile.MobileExclusive.packedSysex;


/**
 * WaveMessage.
 * <pre>
 *  format 0x00
 *   duration   1or2
 *   event      cc oo nnnn
 *              ~~ ~~ ~~~~
 *              |  |  +--- number
 *              |  +------ octave
 *              +--------- channel
 *   gateTime   1or2
 * </pre>
 * system property
 * <li>{@code vavi.sound.mobile.AudioEngine.disabled} ... not to use vavi.sound.mobile.AudioEngine but
 * to send {@link MobileExclusive}s to the synthesizer, default {@code false}</li>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071009 nsano initial version <br>
 */
public class WaveMessage extends SmafMessage
    implements WaveSequencer, MidiConvertible, Serializable {

    private static final Logger logger = getLogger(WaveMessage.class.getName());

    /** smaf channel 0 ~ 3 */
    private int channel;

    /** */
    private int number;

    /** */
    private int gateTime;

    /**
     * for reading
     *
     * @param duration
     * @param data
     * @param gateTime
     */
    public WaveMessage init(int duration, int data, int gateTime) {
        this.duration = duration;
        this.channel = (data & 0xc0) >> 6;
        this.number = data & 0x3f;
        this.gateTime = gateTime;

        return this;
    }

    /**
     * for writing
     *
     * @param duration
     * @param channel smaf channel
     * @param number
     * @param gateTime
     */
    public WaveMessage init(int duration, int channel, int number, int gateTime) {
        this.duration = duration;
        this.channel = channel;
        this.number = number;
        this.gateTime = gateTime;

        return this;
    }

    /** */
    public int getChannel() {
        return channel;
    }

    /** */
    public int getNumber() {
        return number;
    }

    /** */
    public int getGateTime() {
        return gateTime;
    }

    @Override
    public String toString() {
        return "Wave:" +
            " duration=" + duration +
            " channel=" + channel  +
            " number=" + number  +
            " gateTime=" + "%04x".formatted(gateTime);
    }

    // ----

    @Override
    public byte[] getMessage() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FormatType formatType = FormatType.HandyPhoneStandard; // TODO
        switch (formatType) {
        case HandyPhoneStandard:
            try {
                MidiUtil.writeVarInt(new DataOutputStream(baos), duration);
            } catch (IOException e) {
                assert false;
            }
            int event = 0;
            event |= (channel & 0x03) << 6;
            event |= number & 0x3f;
            baos.write(event);
            try {
                MidiUtil.writeVarInt(new DataOutputStream(baos), gateTime);
            } catch (IOException e) {
                assert false;
            }
            break;
        case MobileStandard_Compress:
        case MobileStandard_NoCompress:
        default:
            throw new UnsupportedOperationException("not implemented"); // TODO
//            break;
        }
        return baos.toByteArray();
    }

    @Override
    public int getLength() {
        return getMessage().length;
    }

    /**
     * <p>
     * Create {@link SysexMessage} of Meta type 0x7f as a MIDI message corresponding to
     * this instance of {@link WaveMessage}.
     * and store the numbered ID in 2 bytes big endian.
     * </p>
     * <p>
     * For playback, listen to Sysex manufacturer id 0x45 with {@link VaviSmafReceiver}.
     * Apply it to {@link WaveSequencer} for playback processing.
     * </p>
     * <p>
     * See {@link VaviSmafReceiver} for the playback mechanism.
     * </p>
     * <pre>
     * MIDI Systex should be
     * +--+--+--+--+--+--+--+--+--+--+--+-
     * |f0|45|ID|DD DD ...
     * +--+--+--+--+--+--+--+--+--+--+--+-
     *  0x45 manufacturer id
     *  ID function ID
     * </pre>
     * @see vavi.sound.midi.VaviMidiDeviceProvider#MANUFACTURER_ID
     * @see WaveSequencer#SMAF_SYSEX_FUNCTION_ID_WAVE
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        this.midiGateTimeTicks = context.getTickOfGateTime(gateTime);
logger.log(Level.DEBUG, "midiGateTimeTics: " + midiGateTimeTicks);

        // the synthesizer plays the wave, it is told when to start and when to stop,
        // the wave itself came as a WaveDataMessage of the same number.
        // the start carries the gate time too: the receiver side is stateless (a new
        // instance per exclusive) and an audio engine cuts the wave frame exactly by it,
        // waiting for the stop would let a whole sample block the adpcm line.
        // gate time ticks are [ms], see MidiContext#getResolution
        return new MidiEvent[] {
            new MidiEvent(packedSysex(on(SMAF_SYSEX_FUNCTION_ID_WAVE, number, 127, channel, midiGateTimeTicks)), context.getCurrentTick()),
            new MidiEvent(packedSysex(off(SMAF_SYSEX_FUNCTION_ID_WAVE, number)), context.getCurrentTick() + midiGateTimeTicks)
        };
    }

    private long midiGateTimeTicks;

    /**
     * @param data on  11 id vl ch g2 g1 g0
     *             off 12 id ... the gate time of the start already stops it
     * @throws IllegalArgumentException when audio engine does not found
     * @see MobileExclusive#on
     */
    @Override
    public void sequence(byte[] data, Receiver receiver) throws InvalidSmafDataException {
        assert data[0] == 0x11 || data[0] == 0x12 : "illegal command";
        int command = data[0];

        if (command == 0x11) {
            int id = data[1] & 0x7f;
            long gateTime = MobileExclusive.gateTime(data);
            // resolve here: the engine is held in a ThreadLocal set on this (receiver) thread
            AudioEngine engine = AudioEngineFactory.getAudioEngine();
logger.log(Level.DEBUG, "WAVE PLAY: " + id + ", gate: " + gateTime + " ms, delay: " + AudioEngine.Sync.getDelay() + " ms");
            AudioEngine.Sync.schedule(() -> engine.start(id, gateTime));
        }
    }
}
