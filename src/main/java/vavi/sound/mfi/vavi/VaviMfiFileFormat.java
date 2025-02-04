/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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

import static java.lang.System.getLogger;

import static vavi.sound.mfi.vavi.VaviMfiFileFormat.DumpContext.getDC;


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
 *  number of tracks    0C 01   01:4 voices, 02:8 voices(MFi2), 04:16 voices(MFi2)
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

    private static final Logger logger = getLogger(VaviMfiFileFormat.class.getName());

    /**
     * MIDI file type
     * @see "vavi/sound/midi/package.html"
     */
    public static final int FILE_TYPE = 0x88;

    /** MFi data store of this class */
    private final Sequence sequence;

    /** */
    private HeaderChunk headerChunk;

    List<AudioDataMessage> audioDataChunks = new ArrayList<>();

    List<TrackMessage> trackChunks = new ArrayList<>();

    /** Gets MFi data */
    public Sequence getSequence() {
        return sequence;
    }

    /** for reading */
    private VaviMfiFileFormat() {
        super(FILE_TYPE, -1);

        this.sequence = new Sequence();
    }

    /** for writing */
    public VaviMfiFileFormat(Sequence sequence) {
        super(FILE_TYPE, -1);

        this.sequence = sequence;
        // retrieve header information
        this.headerChunk = new HeaderChunk(new HeaderChunk.Support() {
            @Override
            public void init(Map<String, SubMessage> subChunks) {
                Track track = VaviMfiFileFormat.this.sequence.getTracks()[0];
                for (int j = 0; j < track.size(); j++) {
                    MfiEvent event = track.get(j);
                    MfiMessage message = event.getMessage();
                    if (message instanceof SubMessage subChunk) {
                        //logger.log(Level.TRACE, infoMessage);
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

        // whole file in brief
        this.byteLength = headerChunkLength + audioChunksLength + trackChunksLength;
    }

    /** Gets the total length of all track chunks. */
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
     * Gets the total length of all audio data chunks.
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
logger.log(Level.DEBUG, "audioDataLength: " + audioDataLength);
        return audioDataLength;
    }

    /**
     * Gets all audio data chunks.
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

    /** types of messages omitted when exporting with {@link Track}[0] */
    static boolean isIgnored(MfiMessage message) {
        // TODO is it ideal to omit just MetaMessage?
        return message instanceof SubMessage || message instanceof AudioDataMessage;
    }

    /**
     * Write to the stream. Set the sequence in advance.
     * @after {@link #byteLength} will be set
     * @after <code>os</code> will be {@link java.io.OutputStream#flush() flush}
     * @throws IllegalStateException when sequence is not set
     * @throws InvalidMfiDataException minimum {@link SubMessage}
     *         { {@link #setSorc(int) "sorc"},
     *         {@link #setTitle(String) "titl"},
     *         {@link #setVersion(String) "vers"} }
     *         are not set
     */
    public void writeTo(OutputStream os) throws InvalidMfiDataException, IOException {

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

        os.flush(); // TODO is this needed?
    }

    /**
     * Gets a {@link MfiFileFormat} object from the stream.
     * {@link Sequence} will be created, so use {@link #getSequence()} to retrieve it and use it.
     * @param is MFi stream
     * @return {@link VaviMfiFileFormat} object
     * @throws InvalidMfiDataException at the beginning of 4 bytes is not {@link HeaderChunk#TYPE}
     */
    public static VaviMfiFileFormat readFrom(InputStream is) throws InvalidMfiDataException, IOException {

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
        mff.audioDataChunks = new ArrayList<>();
int dataLength = mff.headerChunk.getMfiDataLength() - (2 + mff.headerChunk.getDataLength());
int l = 0;

        // 2. audio data
        for (int audioDataNumber = 0; audioDataNumber < audioDataCount; audioDataNumber++) {
logger.log(Level.DEBUG, "audio data number: " + audioDataNumber);

            AudioDataMessage audioDataChunk = new AudioDataMessage(audioDataNumber);
            audioDataChunk.readFrom(is);

            mff.audioDataChunks.add(audioDataChunk);

l += audioDataChunk.getLength();
logger.log(Level.DEBUG, "adat length sum: " + l + " / " + dataLength);
        }

        // 3. track
        for (int trackNumber = 0; trackNumber < tracksCount; trackNumber++) {
logger.log(Level.DEBUG, "track number: " + trackNumber);

            Track track = mff.sequence.createTrack();

            if (trackNumber == 0) {
                // TODO this should be done when conversion
                doSpecial(headerSubChunks, mff.audioDataChunks, track);
            }

            // normal process
            TrackMessage trackChunk = new TrackMessage(trackNumber, track);
            trackChunk.setNoteLength(noteLength);
            trackChunk.setExst(exst);
            trackChunk.readFrom(is);

            mff.trackChunks.add(trackChunk);
l += trackChunk.getLength();
logger.log(Level.DEBUG, "trac length sum: " + l + " / " + dataLength);
        }

logger.log(Level.DEBUG, "is rest: " + is.available());
        return mff;
    }

    /**
     * Special process to {@link Track} 0.
     * TODO i don't like separation like this...
     * @param headerSubChunks source 1
     * @param audioDataChunks source 2
     * @param track dest, must be track 0 and empty
     */
    private static void doSpecial(Map<String, SubMessage> headerSubChunks,
                                  List<AudioDataMessage> audioDataChunks,
                                  Track track) {
        // insert SubMessage at top of Track 0
        // TODO it seems to be done in HeaderChunk???
        for (SubMessage headerSubChunk : headerSubChunks.values()) {
            track.add(new MfiEvent(headerSubChunk, 0L));
        }

        // insert AudioDataMessage at next header sub chunks of Track 0
        for (AudioDataMessage audioDataChunk : audioDataChunks) {
            // TODO convert to {@link MetaMessage}???
            track.add(new MfiEvent(audioDataChunk, 0L));
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
logger.log(Level.INFO, "no note info, use 0");
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
     * @throws NoSuchElementException when the sorc chunk is not found
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
    public void setSorc(int sorc) throws InvalidMfiDataException {

        SorcMessage subChunk = (SorcMessage) headerChunk.getSubChunks().get(SorcMessage.TYPE);
        if (subChunk != null) {
            subChunk.setSorc(sorc);
        } else {
            headerChunk.getSubChunks().put(SorcMessage.TYPE, new SorcMessage(sorc));
        }
    }

    /**
     * @throws NoSuchElementException when a title chunk is not found
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
    public void setTitle(String title) throws InvalidMfiDataException {

        TitlMessage subChunk = (TitlMessage) headerChunk.getSubChunks().get(TitlMessage.TYPE);
        if (subChunk != null) {
            subChunk.setTitle(title);
        } else {
            headerChunk.getSubChunks().put(TitlMessage.TYPE, new TitlMessage(title));
        }
    }

    /**
     * @throws NoSuchElementException when a version chunk is not found
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
    public void setVersion(String version) throws InvalidMfiDataException {

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
    public void setProt(String prot) throws InvalidMfiDataException {

        ProtMessage subChunk = (ProtMessage) headerChunk.getSubChunks().get(ProtMessage.TYPE);
        if (subChunk != null) {
            subChunk.setProt(prot);
        } else {
            headerChunk.getSubChunks().put(ProtMessage.TYPE, new ProtMessage(prot));
        }
    }

    /**
     * Gets Extended Status A length.
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
     * Sets Extended Status A length.
     * @see ExstMessage
     */
    public void setExst(int exst) throws InvalidMfiDataException {

        ExstMessage subChunk = (ExstMessage) headerChunk.getSubChunks().get(ExstMessage.TYPE);
        if (subChunk != null) {
            subChunk.setExst(exst);
        } else {
            headerChunk.getSubChunks().put(ExstMessage.TYPE, new ExstMessage(exst));
        }
    }

    /**
     * Gets AudioDataChunk count.
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
     * Whether does it consist of AudioDataChunk only?
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

    // ----

    /** indentation management */
    static class DumpContext implements AutoCloseable /* i know this is abuse. */ {
        /** indentation management store */
        private static ThreadLocal<DumpContext> dc = new ThreadLocal<>();

        static final String indent = " ".repeat(4);
        int depth = 0;
        String indent() { return indent.repeat(depth); }
        DumpContext open() { depth++; return this; }
        @Override public void close() { depth--; }

        /** Gets indentation manager */
        public static DumpContext getDC() {
            if (dc.get() == null)
                dc.set(new DumpContext());
            return dc.get();
        }

        /** Gets indented string. */
        String format(String x) { return getDC().indent() + " +--- " + x + "\n"; }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        try (var dc = getDC().open()) {
            sb.append(headerChunk);
        }
        audioDataChunks.forEach(adc -> sb.append(getDC().format(adc.toString())));
        trackChunks.forEach(tc -> sb.append(getDC().format(tc.toString())));
        return sb.toString();
    }
}
