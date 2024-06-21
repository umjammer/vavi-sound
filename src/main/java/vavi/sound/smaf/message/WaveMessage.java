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
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

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
 * TODO isn't it something like SysexMessage?
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

    /** */
    public String toString() {
        return "Wave:" +
            " duration=" + duration +
            " channel=" + channel  +
            " number=" + number  +
            " gateTime=" + String.format("%04x", gateTime);
    }

    //----

    /* */
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

    /* */
    @Override
    public int getLength() {
        return getMessage().length;
    }

    /**
     * <p>
     * Create {@link MetaMessage} of Meta type 0x7f as a MIDI message corresponding to
     * this instance of {@link WaveMessage}.
     * Store this instance of {@link WaveMessage} in {@link SmafMessageStore}
     * as the actual data of {@link MetaMessage}
     * and store the numbered ID in 2 bytes big endian.
     * </p>
     * <p>
     * For playback, listen to Meta type 0x7f with {@link javax.sound.midi.MetaEventListener}
     * and find the message with the corresponding id from {@link SmafMessageStore}.
     * Apply it to {@link vavi.sound.smaf.sequencer.WaveSequencer} for playback processing.
     * </p>
     * <p>
     * See vavi.sound.smaf.MetaEventAdapter for the playback mechanism.
     * </p>
     * <pre>
     * MIDI Meta
     * +--+--+--+--+--+--+--+--+--+--+--+-
     * |ff|7f|LL|ID|DD DD ...
     * +--+--+--+--+--+--+--+--+--+--+--+-
     *  0x7f sequencer specific meta event
     *  LL really 1 byte?
     *  ID manufacturer ID
     * </pre>
     * <pre>
     * current specs.
     * +--+--+--+--+--+--+--+
     * |ff|7f|LL|5f|01|DH DL|
     * +--+--+--+--+--+--+--+
     *  0x5f manufacturer ID added arbitrarily
     *  0x01 indicates {@link WaveMessage}  data
     *  DH DL numbered id
     * </pre>
     * @see vavi.sound.midi.VaviMidiDeviceProvider#MANUFACTURER_ID
     * @see vavi.sound.smaf.sequencer.WaveSequencer#META_FUNCTION_ID_SMAF
     */
    @Override
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
        metaMessage.setMessage(0x7f,    // sequencer specific meta event
                               data,
                               data.length);

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrentTick())
        };
    }

    /* */
    @Override
    public void sequence() throws InvalidSmafDataException {
logger.log(Level.DEBUG, "WAVE PLAY: " + number);
        AudioEngine engine = Factory.getAudioEngine();
        engine.start(number);
    }
}
