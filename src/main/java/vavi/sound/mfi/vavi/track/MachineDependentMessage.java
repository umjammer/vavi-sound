/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.SysexMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;
import vavi.sound.mfi.vavi.sequencer.MfiMessageStore;
import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Machine dependent System exclusive message.
 * <pre>
 *  0xff, 0xff
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020703 nsano refine <br>
 *          0.01 030711 nsano add constants <br>
 *          0.02 030711 nsano add {@link #getCarrier()} <br>
 *          0.03 030712 nsano read length as unsigned <br>
 *          0.04 030820 nsano implements {@link Serializable} <br>
 *          0.05 030821 nsano implements {@link MidiConvertible} <br>
 */
public class MachineDependentMessage extends SysexMessage
    implements MidiConvertible, Serializable {

    /** */
    protected MachineDependentMessage(byte[] message) {
        super(message);
    }

    /** */
    public MachineDependentMessage() {
        super(new byte[0]);
    }

    /**
     * メッセージを設定します。データは6バイト目からのもの(実際のデータ)を指定します。
     * @param delta delta time
     * @param message data from 6th byte
     */
    public void setMessage(int delta, byte[] message)
        throws InvalidMfiDataException {

        byte[] tmp = new byte[5 + message.length];
//Debug.println("data: " + message.length);
        tmp[0] = (byte) (delta & 0xff);
        tmp[1] = (byte) 0xff;
        tmp[2] = (byte) 0xff;
        tmp[3] = (byte) ((message.length / 0x100) & 0xff);
        tmp[4] = (byte) ((message.length % 0x100) & 0xff);
//Debug.dump(new ByteArrayInputStream(tmp, 0, 5));
        System.arraycopy(message, 0, tmp, 5, message.length);

//Debug.println("message: " + tmp.length);
        super.setMessage(tmp, tmp.length);
//Debug.dump(new ByteArrayInputStream(this.data, 0, 10));
    }

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param is 実際のデータ (ヘッダ無し, data2 ~)
     */
    public static MachineDependentMessage readFrom(int delta, int status, int data1, InputStream is)
        throws InvalidMfiDataException,
               IOException {

//Debug.dump(is);
        DataInputStream dis = new DataInputStream(is);

        int length = dis.readUnsignedShort();
//Debug.println("length: " + length);

        byte[] data = new byte[length + 5];

        data[0] = (byte) (delta & 0xff);
        data[1] = (byte) 0xff;                      // normal 0xff
        data[2] = (byte) 0xff;                      // machine depend 0xff
        data[3] = (byte) ((length / 0x100) & 0xff); // length LSB
        data[4] = (byte) ((length % 0x100) & 0xff); // lenght MSB

        dis.readFully(data, 5, length);

        // 0 delta
        // 5 vendor | carrier
        // 6
        // 7
Debug.println("MachineDepend: " + StringUtil.toHex2(data[0]) + ", " + StringUtil.toHex2(data[5]) + " " + StringUtil.toHex2(data[6]) + " " + StringUtil.toHex2(data[7]) + " " + (data.length > 8 ? StringUtil.toHex2(data[8]) : "") + " " + (data.length > 9 ? StringUtil.toHex2(data[9]) : "") + " " + (data.length > 10 ? StringUtil.toHex2(data[10]) : ""));
        MachineDependentMessage message = new MachineDependentMessage(data);
        return message;
    }

    /** */
    public int getVendor() {
        return data[5] & 0xf0;
    }

    /** */
    public int getCarrier() {
        return data[5] & 0x0f;
    }

    /** */
    public String toString() {
        return "MachineDepend: " +
            "vendor: " + StringUtil.toHex2(data[5] & 0xff);
    }

    //----

    /**
     * <p>
     * この {@link MachineDependentMessage} のインスタンスに対応する
     * MIDI メッセージとして Meta type 0x7f の {@link MetaMessage} を作成する。
     * {@link MetaMessage} の実データとして {@link MfiMessageStore}
     * にこの {@link MachineDependentMessage} のインスタンスをストアして採番された id を
     * 2 bytes big endian で格納する。
     * </p>
     * <p>
     * 再生の場合は {@link javax.sound.midi.MetaEventListener} で Meta type 0x7f を
     * リッスンして対応する id のメッセージを {@link MfiMessageStore} から見つける。
     * それを {@link vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer} にかけて再生処理を
     * 行う。
     * </p>
     * <p>
     * 再生機構は {@link vavi.sound.mfi.vavi.MetaEventAdapter} を参照。
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
     *  0x01 {@link MachineDependentMessage} データであることを表す
     *  DH DL 採番された id
     * </pre>
     * <p>
     * デフォルトの MIDI シーケンサを使用するため、メタイベントしかフックできないので
     * メタイベントに変換している。
     * </p>
     * @see vavi.sound.midi.VaviMidiDeviceProvider#MANUFACTURER_ID
     * @see MachineDependentSequencer#META_FUNCTION_ID_MACHINE_DEPEND
     */
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        MetaMessage metaMessage = new MetaMessage();

        int id = MfiMessageStore.put(this);
        byte[] data = {
            VaviMidiDeviceProvider.MANUFACTURER_ID,
            MachineDependentSequencer.META_FUNCTION_ID_MACHINE_DEPEND,
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
}

/* */
