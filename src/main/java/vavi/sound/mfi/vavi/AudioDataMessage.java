/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.vavi.audio.AdpmMessage;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mfi.vavi.sequencer.MfiMessageStore;
import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.mobile.AudioEngine;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * AudioDataMessage.
 * <pre>
 *  adat
 *   type       4       "adat"
 *   length     4       
 *   header     x 1     *1
 *  
 *  header (*1)
 *   length     2
 *   format     1
 *   attribute  1
 *   sub chunk  x N     *2
 *  
 *  sub chunk (*2)
 *   type       4
 *   length     2
 *   data       L
 * </pre>
 * <li>{@link #data} には header, sub chunk 部分は含まれていません。内容は純粋な ADPCM っぽい
 * <li>{@link #length} は AudioData Chunk すべての長さ
 * <li>TODO extends {@link MfiMessage} いるの？ AudioDataChunk じゃないの？
 * <li>TODO このクラスそのまま {@link vavi.sound.mfi.Track} に入れたらよくないのでは？ → extends {@link MetaMessage}？
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050721 nsano initial version <br>
 * @since MFi 4.0
 */
public class AudioDataMessage extends MfiMessage
    implements MidiConvertible, AudioDataSequencer {

    /** {@value} */
    public static final String TYPE = "adat";

    /** "adat" の index */
    private int audioDataNumber;

    /**
     * @since MFi 5.0 
     */
    public static final int FORMAT_ADPCM_TYPE2 = 0x81;

    /**
     * @see #FORMAT_ADPCM_TYPE2
     */
    private int format;

    /**
     * <pre>
     * 76543 2 1 0
     * ~~~~~ ~ ~ ~
     *     | | | +- 音程変化制御識別子 (0: 音程変化に影響されない, 1: 音程変化に影響される)
     *     | | +--- テンポ変化制御識別子 (0: テンポ変化に影響されない, 1: テンポ変化に影響される)
     *     | +----- 3D 識別子 (0: 3D 処理していない, 1: 3D 処理している)
     *     +------- 予約 (0 固定)
     * </pre>
     */
    private int attribute;

    /** */
    private Map<String, SubMessage> subChunks = new LinkedHashMap<String, SubMessage>();

    /**
     * @see #FORMAT_ADPCM_TYPE2
     */
    public int getFormat() {
        return format;
    }

    /** */
    public boolean is3D() {
        return (attribute & 0x04) != 0;
    }

    /**
     * @return Returns the index.
     */
    public int getAudioDataNumber() {
        return audioDataNumber;
    }

    /** for writer */
    public AudioDataMessage(int format, int attribute, SubMessage ... subChunks) {
        super(new byte[0]);
        this.format = format;
        this.attribute = attribute;
        for (SubMessage subChunk : subChunks) {
            this.subChunks.put(subChunk.getSubType(), subChunk);
        }
    }

    /** for reader */
    public AudioDataMessage(int index) {
        super(new byte[0]);
        this.audioDataNumber = index;
    }

    /** */
    public void writeTo(OutputStream os)
        throws IOException {

        // 1. recalc
        int dataLength = data.length;
Debug.println("dataLength: " + dataLength);
        int subChunksLength = 0;
        for (SubMessage subChunk : subChunks.values()) {
            subChunksLength += 4 + 2 + subChunk.getDataLength(); // type + length + ...
        }
Debug.println("subChunksLength: " + subChunksLength);
        int headerLength = 1 + 1 + subChunksLength; // format + attribute + ...
        int audioDataLength = 2 + headerLength + dataLength; // headerLength + ...
Debug.println("audioDataLength: " + audioDataLength);

        // 2. write
        DataOutputStream dos = new DataOutputStream(os);

        dos.writeBytes(TYPE);
        dos.writeInt(audioDataLength);

        dos.writeShort(headerLength);
        dos.writeByte(format);
        dos.writeByte(attribute);
        for (SubMessage subChunk : subChunks.values()) {
            subChunk.writeTo(os);
        }

        dos.write(data);
    }

    /**
     * @after {@link #length} が設定されます、AudioData Chunk すべての長さ
     * @after {@link #data} が設定されます、ヘッダ部、サブチャンクは含まれない
     * @throws InvalidMfiDataException is の始まりが {@link #TYPE} で始まっていない場合
     */
    public void readFrom(InputStream is)
        throws InvalidMfiDataException,
               IOException {

        DataInputStream dis = new DataInputStream(is);

        // type
        byte[] bytes = new byte[4];
        dis.read(bytes, 0, 4);
        String string = new String(bytes);
        if (!TYPE.equals(string)) {
            throw new InvalidMfiDataException("invalid audio data: " + string);
        }

        // length
        int audioDataLength = dis.readInt();

        // header
        int headerLength = dis.readUnsignedShort();
        this.format = dis.readUnsignedByte();
        this.attribute = dis.readUnsignedByte();
Debug.println(String.format("adat header: %d: f: %02x, a: %02x", headerLength, format, attribute));

        // sub chunks
        int l = 0;
        while (l < headerLength - (1 + 1)) { // - (format + attribute)
            SubMessage subChunk = SubMessage.readFrom(is);
            subChunks.put(subChunk.getSubType(), subChunk);
            l += subChunk.getDataLength() + 4 + 2; // + type + length
Debug.println("audio subchunk length sum: " + l + " / " + (headerLength - 2));
        }

        // data
        int dataLength = audioDataLength - (headerLength + 1 + 1); // + format + attribute
        data = new byte[dataLength]; // TODO 全部のデータ含めるべき
        dis.readFully(data, 0, dataLength);
Debug.println("adat length[" + audioDataNumber + "]: " + dataLength + " bytes\n" + StringUtil.getDump(data, 16));

        //
        this.length = audioDataLength + 4 + 4; // + type + length
    }

    /**
     * ヘッダ、サブチャンク除く。(純粋な ADPCM)
     * データインターリーブは行わず L R の順にまとめて格納する
     * <li>TODO Chunk interface
     */
    public void setData(byte[] data) {
        this.data = data;

        // calc
        int dataLength = data.length;
Debug.println("dataLength: " + dataLength);
        int subChunksLength = 0;
        for (SubMessage subChunk : subChunks.values()) {
            subChunksLength += 4 + 2 + subChunk.getDataLength(); // type + length + ...
        }
Debug.println("subChunksLength: " + subChunksLength);
        int headerLength = 1 + 1 + subChunksLength; // format + attribute + ...
        int audioDataLength = 2 + headerLength + dataLength; // headerLength + ...
Debug.println("audioDataLength: " + audioDataLength);
        this.length = audioDataLength + 4 + 4; // + type + length
    }

    /**
     * ヘッダ、サブチャンク除く。 (純粋な ADPCM)
     * データインターリーブは行わず L R の順にまとめて格納されている
     * <li>TODO Chunk interface
     */
    public byte[] getData() {
        return data;
    }

    /** @throws UnsupportedOperationException */
    public byte[] getMessage() {
        throw new UnsupportedOperationException("no mean");
    }

    //----

    /** */
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
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
        int id = getAudioDataNumber();
        int format = getFormat();
        byte[] data = getData();

        AdpmMessage adpm = (AdpmMessage) subChunks.get(AdpmMessage.TYPE);
        int samplingRate = adpm.getSamplingRate() * 1000;
        int samplingBits = adpm.getSamplingBits();
        int channels = adpm.getChannels();

        AudioEngine engine = Factory.getAudioEngine(format);
        engine.setData(id, -1, samplingRate, samplingBits, channels, data, false);
    }
}

/* */
