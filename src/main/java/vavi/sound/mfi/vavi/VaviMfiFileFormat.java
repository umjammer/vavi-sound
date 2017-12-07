/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiFileFormat;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.Track;
import vavi.sound.mfi.vavi.header.AinfMessage;
import vavi.sound.mfi.vavi.header.ExstMessage;
import vavi.sound.mfi.vavi.header.NoteMessage;
import vavi.sound.mfi.vavi.header.ProtMessage;
import vavi.sound.mfi.vavi.header.SorcMessage;
import vavi.sound.mfi.vavi.header.TitlMessage;
import vavi.sound.mfi.vavi.header.VersMessage;
import vavi.util.Debug;


/**
 * MFi file format.
 *
 * <pre>
 * -- top of file --
 * 1. file header       13 bytes
 * 2. data information  1
 *    data information  2
 *                      : (max number of informations: MFi 6. MFi2 8)
 * 3. tracks
 * -- end of file --
 *
 * 1. file header
 *  type                00 04   "melo"
 *  data length         04 04   file length - 8
 *  offset to tracks    08 02
 *  major type          0A 01   see below
 *  minor type          0B 01   see below
 *  number of tracks    0C 01   01:4和音, 02:8和音(MFi2), 04:16和音(MFi2)
 *
 * 2. data information
 *  type                00 04   see below *1
 *  data length         04 02   n
 *  data                06 n
 *
 *  *1 type
 *   "titl"     n       mld title, < 16 bytes expected, SJIS encoded
 *   "sorc"     1       protect information *2
 *   "vers"     4       mld version
 *   "date"     8       date created
 *   "copy"     n       copyright
 *   "prot"     n       data managing
 *   "note"     2       note length (1 for 4byte)
 *   "exst"     2       extended status data length
 *
 *  *2 sorc
 *    msb 7    0000000    from network
 *             0000001    from terminal
 *             0000010    from external i/f
 *    lsb    0: no copyright, 1: has copyright
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 020630 nsano refine <br>
 *          0.02 030606 nsano change error trap <br>
 *          0.03 031126 nsano fix info length <br>
 */
public class VaviMfiFileFormat extends MfiFileFormat {

    /**
     * MIDI ファイルタイプ
     * @see "vavi/sound/midi/package.html"
     */
    public static final int FILE_TYPE = 0x88;

    /** MFi data store of this class */
    private Sequence sequence;

    /** */
    private HeaderChunk headerChunk;

    /** Gets MFi data */
    public Sequence getSequence() {
        return sequence;
    }

    /** 読み込み用 */
    private VaviMfiFileFormat() {
        super(FILE_TYPE, -1);

        this.sequence = new Sequence();
    }

    /** 書き込み用 */
    public VaviMfiFileFormat(Sequence sequence) {
        super(FILE_TYPE, -1);

        this.sequence = sequence;
        // ヘッダ情報の取り出し
        this.headerChunk = new HeaderChunk(new HeaderChunk.Support() {
            @Override
            public void init(Map<String, SubMessage> subChunks) {
                Track track = VaviMfiFileFormat.this.sequence.getTracks()[0];
                for (int j = 0; j < track.size(); j++) {
                    MfiEvent event = track.get(j);
                    MfiMessage message = event.getMessage();
                    if (message instanceof SubMessage) {
                        SubMessage subChunk = (SubMessage) message;
//Debug.println(infoMessage);
                        subChunks.put(subChunk.getSubType(), subChunk);
                    }
                }
            }
            @Override
            public int getTracksLength() {
                return getAudioDataLength();
            }
            @Override
            public int getTracksCount() {
                return getTracksLength();
            }
            @Override
            public int getAudioDataLength() {
                return VaviMfiFileFormat.this.sequence.getTracks().length;
            }
        });

        // 1. header (type + length + headerChunkDataLength + ...)
        int headerChunkLength = 4 + 4 + 2 + HeaderChunk.HEADER_LENGTH + headerChunk.getSubChunksLength();
        // 2. audio data
        int audioChunksLength = getAudioDataLength();
        // 3. track
        int trackChunksLength = getTracksLength();

        // 要するにファイル全部
        this.byteLength = headerChunkLength + audioChunksLength + trackChunksLength;
    }

    /** すべてのトラックチャンクの合計の長さを取得します。 */
    private int getTracksLength() {
        Track[] tracks = sequence.getTracks();
        int tracksLength = 0;
        for (int t = 0; t < tracks.length; t++) {
            TrackMessage track = new TrackMessage(t, tracks[t]);
            tracksLength += track.getDataLength() + 4 + 4; // ... + type + length
        }
        return tracksLength;
    }

    /**
     * すべてのオーディオデータチャンクの合計の長さを取得します。
     * @since MFi 4.0
     */
    private int getAudioDataLength() {
        int audioDataLength = 0;
        Track track = sequence.getTracks()[0];
        for (int j = 0; j < track.size(); j++) {
            MfiEvent event = track.get(j);
            MfiMessage message = event.getMessage();
            if (message instanceof AudioDataMessage) {
                audioDataLength += message.getLength();
            }
        }
Debug.println("audioDataLength: " + audioDataLength);
        return audioDataLength;
    }

    /**
     * すべてのオーディオデータチャンクを取得します。
     * @since MFi 4.0
     */
    private List<AudioDataMessage> getAudioDatum() {
        List<AudioDataMessage> result = new ArrayList<>();
        Track track = sequence.getTracks()[0];
        for (int j = 0; j < track.size(); j++) {
            MfiEvent event = track.get(j);
            MfiMessage message = event.getMessage();
            if (message instanceof AudioDataMessage) {
                result.add((AudioDataMessage) message);
            }
        }
        return result;
    }

    /** {@link Track}[0] で書き出し時省かれるメッセージの型 */
    static boolean isIgnored(MfiMessage message) {
        // TODO MetaMessage だけを省くのが理想？
        return message instanceof SubMessage || message instanceof AudioDataMessage;
    }

    /**
     * ストリームに書き込みます。事前にシーケンスを設定しておくこと。
     * @after {@link #byteLength} が設定されます
     * @after os は {@link java.io.OutputStream#flush() flush} されます
     * @throws IllegalStateException シーケンスが設定されていない場合スローされます
     * @throws InvalidMfiDataException 最低限の {@link SubMessage}
     *         { {@link #setSorc(int) "sorc"},
     *         {@link #setTitle(String) "titl"},
     *         {@link #setVersion(String) "vers"} }
     *         が設定されていない場合スローされます
     */
    public void writeTo(OutputStream os)
        throws InvalidMfiDataException,
               IOException {

        if (sequence == null) {
            throw new IllegalStateException("no sequence");
        }

        // 1. header
        headerChunk.writeTo(os);

        // 2. audio data
        for (AudioDataMessage audioData : getAudioDatum()) {
            audioData.writeTo(os);
        }

        // 3. tracks
        Track[] tracks = sequence.getTracks();
        for (int t = 0; t < tracks.length; t++) {
            TrackMessage track = new TrackMessage(t, tracks[t]);
            track.writeTo(os);
        }

        os.flush(); // TODO いる？
    }

    /**
     * ストリームから {@link MfiFileFormat} オブジェクトを取得します。
     * {@link Sequence} が作成されるので {@link #getSequence()} で取り出して使用します。
     * @param is
     * @return {@link VaviMfiFileFormat} オブジェクト
     * @throws InvalidMfiDataException 最初の 4 bytes が {@link HeaderChunk#TYPE} で無い場合
     */
    public static VaviMfiFileFormat readFrom(InputStream is)
        throws InvalidMfiDataException,
               IOException {

        VaviMfiFileFormat mff = new VaviMfiFileFormat();

        // 1. header
        mff.headerChunk = HeaderChunk.readFrom(is);
        mff.byteLength = 4 + 4 + mff.headerChunk.getMfiDataLength(); // type + length + // TODO use accessory
        int noteLength = mff.getNoteLength();
        int exst = mff.getExst();
        int tracksCount = mff.headerChunk.getTracksCount();
        int audioDataCount = mff.getAudioDataChunkCount();
//      boolean isAudioDataOnly = ff.isAudioDataOnly();
        Map<String, SubMessage> headerSubChunks = mff.headerChunk.getSubChunks();
        List<AudioDataMessage> audioDataChunks = new ArrayList<>();
int dataLength = mff.headerChunk.getMfiDataLength() - (2 + mff.headerChunk.getDataLength());
int l = 0;

        // 2. audio data
        for (int audioDataNumber = 0; audioDataNumber < audioDataCount; audioDataNumber++) {
Debug.println("audio data number: " + audioDataNumber);

            AudioDataMessage audioDataChunk = new AudioDataMessage(audioDataNumber);
            audioDataChunk.readFrom(is);

            audioDataChunks.add(audioDataChunk);

l += audioDataChunk.getLength();
Debug.println("adat length sum: " + l + " / " + dataLength);
        }

        // 3. track
        for (int trackNumber = 0; trackNumber < tracksCount; trackNumber++) {
Debug.println("track number: " + trackNumber);

            Track track = mff.sequence.createTrack();

            if (trackNumber == 0) {
                doSpecial(headerSubChunks, audioDataChunks, track);
            }

            // 通常処理
            TrackMessage trackChunk = new TrackMessage(trackNumber, track);
            trackChunk.setNoteLength(noteLength);
            trackChunk.setExst(exst);
            trackChunk.readFrom(is);

l += trackChunk.getLength();
Debug.println("trac length sum: " + l + " / " + dataLength);
        }

Debug.println("is rest: " + is.available());
        return mff;
    }

    /**
     * {@link Track} 0 に対する特別な処理。
     * TODO こういう分離あまり好きくない...
     * @param headerSubChunks source 1
     * @param audioDataChunks source 2
     * @param track dest, must be track 0 and empty
     */
    private static void doSpecial(Map<String, SubMessage> headerSubChunks,
                                  List<AudioDataMessage> audioDataChunks,
                                  Track track) {
        // Track 0 の先頭に SubMessage を押し込む
        // TODO HeaderChunk ですべき予感？？？
        for (SubMessage headerSubChunk : headerSubChunks.values()) {
            track.add(new MfiEvent(headerSubChunk, 0l));
        }

        // Track 0 の header sub chunks の次に AudioDataMessage を押し込む
        for (AudioDataMessage audioDataChunk : audioDataChunks) {
            // TODO {@link MetaMessage} に変換？？？
            track.add(new MfiEvent(audioDataChunk, 0l));
        }
    }

    /** */
    public int getMajorType() {
        return headerChunk.getMajorType();
    }

    /**
     * @see HeaderChunk#MAJOR_TYPE_MUSIC
     * @see HeaderChunk#MAJOR_TYPE_RING_TONE
     */
    public void setMajorType(int majorType) {
        headerChunk.setMajorType(majorType);
    }

    /** */
    public int getMinorType() {
        return headerChunk.getMinorType();
    }

    /**
     * @see HeaderChunk#MINOR_TYPE_ALL
     * @see HeaderChunk#MINOR_TYPE_MUSIC
     * @see HeaderChunk#MINOR_TYPE_PART
     */
    public void setMinorType(int minorType) {
        headerChunk.setMinorType(minorType);
    }

    /**
     * Length of {@link vavi.sound.mfi.NoteMessage}
     * @return 0: 3 bytes, 1: 4bytes
     * @see NoteMessage
     */
    public int getNoteLength() {
        NoteMessage subChunk = (NoteMessage) headerChunk.getSubChunks().get(NoteMessage.TYPE);
        if (subChunk != null) {
            return subChunk.getNoteLength();
        } else {
Debug.println("no note info, use 0");
            return 0;
        }
    }

    /**
     * Length of {@link vavi.sound.mfi.NoteMessage}
     * @param noteLength 0: 3 bytes, 1: 4bytes
     * @see NoteMessage
     */
    public void setNoteLength(int noteLength) {
        NoteMessage subChunk = (NoteMessage) headerChunk.getSubChunks().get(NoteMessage.TYPE);
        if (subChunk != null) {
            subChunk.setNoteLength(noteLength);
        } else {
            headerChunk.getSubChunks().put(NoteMessage.TYPE, new NoteMessage(noteLength));
        }
    }

    /**
     * @return 0: not protected, 1: protected
     * @throws NoSuchElementException
     * @see SorcMessage
     */
    public int getSorc() {
        SorcMessage subChunk = (SorcMessage) headerChunk.getSubChunks().get(SorcMessage.TYPE);
        if (subChunk != null) {
            return subChunk.getSorc();
        } else {
            throw new NoSuchElementException(SorcMessage.TYPE);
        }
    }

    /**
     * protected or not
     * @param sorc 0: not protected, 1: protected
     * @see SorcMessage
     */
    public void setSorc(int sorc)
        throws InvalidMfiDataException {

        SorcMessage subChunk = (SorcMessage) headerChunk.getSubChunks().get(SorcMessage.TYPE);
        if (subChunk != null) {
            subChunk.setSorc(sorc);
        } else {
            headerChunk.getSubChunks().put(SorcMessage.TYPE, new SorcMessage(sorc));
        }
    }

    /**
     * @throws NoSuchElementException
     * @see TitlMessage
     */
    public String getTitle() {
        TitlMessage subChunk = (TitlMessage) headerChunk.getSubChunks().get(TitlMessage.TYPE);
        if (subChunk != null) {
            return subChunk.getTitle();
        } else {
            throw new NoSuchElementException(TitlMessage.TYPE);
        }
    }

    /**
     * @see TitlMessage
     */
    public void setTitle(String title)
        throws InvalidMfiDataException {

        TitlMessage subChunk = (TitlMessage) headerChunk.getSubChunks().get(TitlMessage.TYPE);
        if (subChunk != null) {
            subChunk.setTitle(title);
        } else {
            headerChunk.getSubChunks().put(TitlMessage.TYPE, new TitlMessage(title));
        }
    }

    /**
     * @throws NoSuchElementException
     * @see VersMessage
     */
    public String getVersion() {
        VersMessage subChunk = (VersMessage) headerChunk.getSubChunks().get(VersMessage.TYPE);
        if (subChunk != null) {
            return subChunk.getVersion();
        } else {
            throw new NoSuchElementException(VersMessage.TYPE);
        }
    }

    /**
     * @param version 4 byte number as string (ex. "0400")
     * @see VersMessage
     */
    public void setVersion(String version)
        throws InvalidMfiDataException {

        VersMessage subChunk = (VersMessage) headerChunk.getSubChunks().get(VersMessage.TYPE);
        if (subChunk != null) {
            subChunk.setVersion(version);
        } else {
            headerChunk.getSubChunks().put(VersMessage.TYPE, new VersMessage(version));
        }
    }

    /**
     * Gets copyright string.
     * @see ProtMessage
     */
    public String getProt() {
        ProtMessage subChunk = (ProtMessage) headerChunk.getSubChunks().get(ProtMessage.TYPE);
        if (subChunk != null) {
            return subChunk.getProt();
        } else {
            throw new NoSuchElementException(ProtMessage.TYPE);
        }
    }

    /**
     * Sets copyright string.
     * @see ProtMessage
     */
    public void setProt(String prot)
        throws InvalidMfiDataException {

        ProtMessage subChunk = (ProtMessage) headerChunk.getSubChunks().get(ProtMessage.TYPE);
        if (subChunk != null) {
            subChunk.setProt(prot);
        } else {
            headerChunk.getSubChunks().put(ProtMessage.TYPE, new ProtMessage(prot));
        }
    }

    /**
     * 拡張ステータス A の長さを取得します。
     * @see ExstMessage
     */
    public int getExst() {
        ExstMessage subChunk = (ExstMessage) headerChunk.getSubChunks().get(ExstMessage.TYPE);
        if (subChunk != null) {
            return subChunk.getExst();
        } else {
            return 0;
        }
    }

    /**
     * 拡張ステータス A の長さを設定します。
     * @see ExstMessage
     */
    public void setExst(int exst)
        throws InvalidMfiDataException {

        ExstMessage subChunk = (ExstMessage) headerChunk.getSubChunks().get(ExstMessage.TYPE);
        if (subChunk != null) {
            subChunk.setExst(exst);
        } else {
            headerChunk.getSubChunks().put(ExstMessage.TYPE, new ExstMessage(exst));
        }
    }

    /**
     * AudioDataChunk の数を取得します。
     * @see AinfMessage
     * @since MFi 4.0
     */
    public int getAudioDataChunkCount() {
        AinfMessage subChunk = (AinfMessage) headerChunk.getSubChunks().get(AinfMessage.TYPE);
        if (subChunk != null) {
            return subChunk.getAudioChunksCount();
        } else {
            return 0;
        }
    }

    /**
     * AudioDataChunk のみで構成されているかどうか。
     * @see AinfMessage
     * @since MFi 4.0
     */
    public boolean isAudioDataOnly() {
        AinfMessage subChunk = (AinfMessage) headerChunk.getSubChunks().get(AinfMessage.TYPE);
        if (subChunk != null) {
            return subChunk.isAudioChunkOnly();
        } else {
            return false;
        }
    }
}

/* */
