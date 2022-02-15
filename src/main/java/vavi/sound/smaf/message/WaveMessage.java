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
import vavi.util.Debug;


/**
 * WaveMessage.
 * TODO SysexMessage とかじゃないの？
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
     * この {@link WaveMessage} のインスタンスに対応する
     * MIDI メッセージとして Meta type 0x7f の {@link MetaMessage} を作成する。
     * {@link MetaMessage} の実データとして {@link SmafMessageStore}
     * にこの {@link WaveMessage} のインスタンスをストアして採番された id を
     * 2 bytes big endian で格納する。
     * </p>
     * <p>
     * 再生の場合は {@link javax.sound.midi.MetaEventListener} で Meta type 0x7f を
     * リッスンして対応する id のメッセージを {@link SmafMessageStore} から見つける。
     * それを {@link vavi.sound.smaf.sequencer.WaveSequencer} にかけて再生処理を
     * 行う。
     * </p>
     * <p>
     * 再生機構は vavi.sound.smaf.MetaEventAdapter を参照。
     * </p>
     * <pre>
     * MIDI Meta
     * +--+--+--+--+--+--+--+--+--+--+--+-
     * |ff|7f|LL|ID|DD DD ...
     * +--+--+--+--+--+--+--+--+--+--+--+-
     *  0x7f シーケンサー固有メタイベント
     *  LL ホンマに 1 byte ？
     *  ID メーカーID
     * </pre>
     * <pre>
     * 現状
     * +--+--+--+--+--+--+--+
     * |ff|7f|LL|5f|01|DH DL|
     * +--+--+--+--+--+--+--+
     *  0x5f 勝手につけたメーカ ID
     *  0x01 {@link WaveMessage} データであることを表す
     *  DH DL 採番された id
     * </pre>
     * @see vavi.sound.midi.VaviMidiDeviceProvider#MANUFACTURER_ID
     * @see vavi.sound.smaf.sequencer.WaveSequencer#META_FUNCTION_ID_SMAF
     */
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
Debug.println("WAVE PLAY: " + number);
        AudioEngine engine = Factory.getAudioEngine();
        engine.start(number);
    }
}

/* */
