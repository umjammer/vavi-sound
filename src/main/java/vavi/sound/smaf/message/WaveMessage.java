/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.SysexMessage;

import vavi.sound.midi.MidiUtil;
import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;
import vavi.sound.smaf.sequencer.SmafMessageStore;
import vavi.sound.smaf.sequencer.WaveSequencer;

import static java.lang.System.getLogger;


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
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071009 nsano initial version <br>
 */
public class WaveMessage extends SmafMessage
    implements WaveSequencer, MidiConvertible, Serializable {

    private static final Logger logger = getLogger(WaveMessage.class.getName());

    /** smaf channel 0 ~ 3 */
    private final int channel;

    /** */
    private final int number;

    /** */
    private final int gateTime;

    /**
     * for reading
     *
     * @param duration
     * @param data
     * @param gateTime
     */
    public WaveMessage(int duration, int data, int gateTime) {
        this.duration = duration;
        this.channel = (data & 0xc0) >> 6;
        this.number = data & 0x3f;
        this.gateTime = gateTime;
    }

    /**
     * for writing
     *
     * @param duration
     * @param channel smaf channel
     * @param number
     * @param gateTime
     */
    public WaveMessage(int duration, int channel, int number, int gateTime) {
        this.duration = duration;
        this.channel = channel;
        this.number = number;
        this.gateTime = gateTime;
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
     * Store this instance of {@link WaveMessage} in {@link SmafMessageStore}
     * as the actual data of {@link SysexMessage}
     * and store the numbered ID in 2 bytes big endian.
     * </p>
     * <p>
     * For playback, listen to Sysex manufacturer id 0x45 with {@link vavi.sound.smaf.SmafSynthesizer.SmafReceiver}
     * and find the message with the corresponding id from {@link SmafMessageStore}.
     * Apply it to {@link vavi.sound.smaf.sequencer.WaveSequencer} for playback processing.
     * </p>
     * <p>
     * See {@link vavi.sound.smaf.SmafSynthesizer.SmafReceiver} for the playback mechanism.
     * </p>
     * <pre>
     * MIDI Systex should be
     * +--+--+--+--+--+--+--+--+--+--+--+-
     * |f0|45|ID|DD DD ...
     * +--+--+--+--+--+--+--+--+--+--+--+-
     *  0x45 manufacturer id
     *  ID function ID
     * </pre>
     * <pre>
     * current specs.
     * +--+--+--+--+--+--+--+
     * |f0|45|01|DH DL|
     * +--+--+--+--+--+--+--+
     *  0x45 manufacturer ID added arbitrarily
     *  0x01 function id, indicates {@link WaveMessage} data
     *  DH DL numbered id
     * </pre>
     * @see vavi.sound.midi.VaviMidiDeviceProvider#MANUFACTURER_ID
     * @see vavi.sound.smaf.sequencer.WaveSequencer#SYSEX_FUNCTION_ID_SMAF
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        this.midiGateTimeTicks = context.getTickOfGateTime(gateTime);
logger.log(Level.INFO, "midiGateTimeTics: " + midiGateTimeTicks);

        SysexMessage sysexMessage = new SysexMessage();

        int id = SmafMessageStore.put(this);
        byte[] data = {
                VaviMidiDeviceProvider.MANUFACTURER_ID, // TODO creating real sysex option
                WaveSequencer.SYSEX_FUNCTION_ID_SMAF,
                (byte) ((id / 0x100) & 0xff),
                (byte) ((id % 0x100) & 0xff)
        };
        sysexMessage.setMessage(0xf0,    // sysex
                               data,
                               data.length);

        return new MidiEvent[] {
            new MidiEvent(sysexMessage, context.getCurrentTick())
        };
    }

    private long midiGateTimeTicks;

    @Override
    public void sequence() throws InvalidSmafDataException {
        // resolve here: the engine is held in a ThreadLocal set on this (receiver) thread
        AudioEngine engine = Factory.getAudioEngine();
logger.log(Level.DEBUG, "WAVE PLAY: " + number + ", delay: " + AudioEngine.Sync.getDelay() + " ms");
        AudioEngine.Sync.schedule(() -> engine.start(number, midiGateTimeTicks));
    }
}
