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
import vavi.sound.mfi.vavi.AudioDataChunk.AudioDataMessage;
import vavi.sound.mfi.vavi.sub.AinfChunk;
import vavi.sound.mfi.vavi.sub.ExstChunk;
import vavi.sound.mfi.vavi.sub.NoteChunk;
import vavi.sound.mfi.vavi.sub.ProtChunk;
import vavi.sound.mfi.vavi.sub.SorcChunk;
import vavi.sound.mfi.vavi.sub.SuptChunk;
import vavi.sound.mfi.vavi.sub.TitlChunk;
import vavi.sound.mfi.vavi.sub.VersChunk;

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
     * @see vavi.sound.midi
     */
    public static final int FILE_TYPE = 0x88;

    /** MFi data store of this class */
    private final Sequence sequence;

    /** */
    private HeaderChunk headerChunk;

    List<AudioDataChunk> audioDataChunks = new ArrayList<>();

    List<TrackChunk> trackChunks = new ArrayList<>();

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
            public void init(Map<String, SubChunk> subChunks) {
                Track track = VaviMfiFileFormat.this.sequence.getTracks()[0];
                for (int j = 0; j < track.size(); j++) {
                    MfiEvent event = track.get(j);
                    MfiMessage message = event.getMessage();
                    if (message instanceof SubChunk subChunk) {
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
            TrackChunk track = new TrackChunk(t, tracks[t]);
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
    private List<AudioDataMessage> getAudioDataMessages() {
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
        // TODO is it ideal to omit just SysexMessage?
        return message instanceof SubChunk || message instanceof AudioDataMessage;
    }

    /**
     * Write to the stream. Set the sequence in advance.
     * @after {@link #byteLength} will be set
     * @after <code>os</code> will be {@link java.io.OutputStream#flush() flush}
     * @throws IllegalStateException when sequence is not set
     * @throws InvalidMfiDataException minimum {@link SubChunk}
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
        for (AudioDataMessage audioData : getAudioDataMessages()) {
            audioData.writeTo(os);
        }

        // 3. tracks
        Track[] tracks = sequence.getTracks();
        for (int t = 0; t < tracks.length; t++) {
            TrackChunk track = new TrackChunk(t, tracks[t]);
            track.writeTo(os);
        }

        os.flush();
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
//        boolean isAudioDataOnly = mff.isAudioDataOnly();
        Map<String, SubChunk> headerSubChunks = mff.headerChunk.getSubChunks();
        mff.audioDataChunks = new ArrayList<>();
int dataLength = mff.headerChunk.getMfiDataLength() - (2 + mff.headerChunk.getDataLength());
int l = 0;

        // 2. audio data
        for (int audioDataNumber = 0; audioDataNumber < audioDataCount; audioDataNumber++) {
logger.log(Level.DEBUG, "audio data number: " + audioDataNumber);

            AudioDataChunk audioDataChunk = new AudioDataChunk(audioDataNumber);
            audioDataChunk.readFrom(is);

            mff.audioDataChunks.add(audioDataChunk);

l += audioDataChunk.getAudioDataMessage().getLength();
logger.log(Level.DEBUG, "adat length sum: " + l + " / " + dataLength);
        }

        // 3. track
        for (int trackNumber = 0; trackNumber < tracksCount; trackNumber++) {
logger.log(Level.DEBUG, "track number: " + trackNumber);
            if (TrackChunk.RottenParser.isLoose() && !TrackChunk.RottenParser.isNextTrack(is)) { // for rotten mfi
logger.log(Level.WARNING, "ignore wrong tracks count: " + tracksCount + " -> " + trackNumber);
                break;
            }

            Track track = mff.sequence.createTrack();

            if (trackNumber == 0) {
                // TODO this should be done when conversion
                doSpecial(headerSubChunks, mff.audioDataChunks, track);
            }

            // normal process
            TrackChunk trackChunk = new TrackChunk(trackNumber, track);
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
    private static void doSpecial(Map<String, SubChunk> headerSubChunks,
                                  List<AudioDataChunk> audioDataChunks,
                                  Track track) {
        // insert SubMessage at top of Track 0
        // TODO it seems to be done in HeaderChunk???
        for (SubChunk headerSubChunk : headerSubChunks.values()) {
            track.add(new MfiEvent(headerSubChunk, 0L));
        }

        // insert AudioDataMessage at next header sub chunks of Track 0
        for (AudioDataChunk audioDataChunk : audioDataChunks) {
            // TODO convert to {@link SysexMessage}???
            track.add(new MfiEvent(audioDataChunk.getAudioDataMessage(), 0L));
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
     * @see NoteChunk
     */
    public int getNoteLength() {
        NoteChunk subChunk = (NoteChunk) headerChunk.getSubChunks().get(NoteChunk.TYPE);
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
     * @see NoteChunk
     */
    public void setNoteLength(int noteLength) {
        NoteChunk subChunk = (NoteChunk) headerChunk.getSubChunks().get(NoteChunk.TYPE);
        if (subChunk != null) {
            subChunk.setNoteLength(noteLength);
        } else {
            headerChunk.getSubChunks().put(NoteChunk.TYPE, new NoteChunk().init(noteLength));
        }
    }

    /**
     * @return 0: not protected, 1: protected
     * @throws NoSuchElementException when the sorc chunk is not found
     * @see SorcChunk
     */
    public int getSorc() {
        SorcChunk subChunk = (SorcChunk) headerChunk.getSubChunks().get(SorcChunk.TYPE);
        if (subChunk != null) {
            return subChunk.getSorc();
        } else {
            throw new NoSuchElementException(SorcChunk.TYPE);
        }
    }

    /**
     * protected or not
     * @param sorc 0: not protected, 1: protected
     * @see SorcChunk
     */
    public void setSorc(int sorc) throws InvalidMfiDataException {

        SorcChunk subChunk = (SorcChunk) headerChunk.getSubChunks().get(SorcChunk.TYPE);
        if (subChunk != null) {
            subChunk.setSorc(sorc);
        } else {
            headerChunk.getSubChunks().put(SorcChunk.TYPE, new SorcChunk().init(sorc));
        }
    }

    /**
     * @throws NoSuchElementException when a title chunk is not found
     * @see TitlChunk
     */
    public String getTitle() {
        TitlChunk subChunk = (TitlChunk) headerChunk.getSubChunks().get(TitlChunk.TYPE);
        if (subChunk != null) {
            return subChunk.getTitle();
        } else {
            throw new NoSuchElementException(TitlChunk.TYPE);
        }
    }

    /**
     * @see TitlChunk
     */
    public void setTitle(String title) throws InvalidMfiDataException {

        TitlChunk subChunk = (TitlChunk) headerChunk.getSubChunks().get(TitlChunk.TYPE);
        if (subChunk != null) {
            subChunk.setTitle(title);
        } else {
            headerChunk.getSubChunks().put(TitlChunk.TYPE, new TitlChunk().init(title));
        }
    }

    /**
     * @throws NoSuchElementException when a version chunk is not found
     * @see VersChunk
     */
    public String getVersion() {
        VersChunk subChunk = (VersChunk) headerChunk.getSubChunks().get(VersChunk.TYPE);
        if (subChunk != null) {
            return subChunk.getVersion();
        } else {
            throw new NoSuchElementException(VersChunk.TYPE);
        }
    }

    /**
     * @param version 4 byte number as string (ex. "0400")
     * @see VersChunk
     */
    public void setVersion(String version) throws InvalidMfiDataException {

        VersChunk subChunk = (VersChunk) headerChunk.getSubChunks().get(VersChunk.TYPE);
        if (subChunk != null) {
            subChunk.setVersion(version);
        } else {
            headerChunk.getSubChunks().put(VersChunk.TYPE, new VersChunk().init(version));
        }
    }

    /**
     * Gets copyright string.
     * @see ProtChunk
     */
    public String getProt() {
        ProtChunk subChunk = (ProtChunk) headerChunk.getSubChunks().get(ProtChunk.TYPE);
        if (subChunk != null) {
            return subChunk.getProt();
        } else {
            throw new NoSuchElementException(ProtChunk.TYPE);
        }
    }

    /**
     * Sets copyright string.
     * @see ProtChunk
     */
    public void setProt(String prot) throws InvalidMfiDataException {

        ProtChunk subChunk = (ProtChunk) headerChunk.getSubChunks().get(ProtChunk.TYPE);
        if (subChunk != null) {
            subChunk.setProt(prot);
        } else {
            headerChunk.getSubChunks().put(ProtChunk.TYPE, new ProtChunk().init(prot));
        }
    }

    /**
     * Gets Extended Status A length.
     * @see ExstChunk
     */
    public int getExst() {
        ExstChunk subChunk = (ExstChunk) headerChunk.getSubChunks().get(ExstChunk.TYPE);
        if (subChunk != null) {
            return subChunk.getExst();
        } else {
            return 0;
        }
    }

    /**
     * Sets Extended Status A length.
     * @see ExstChunk
     */
    public void setExst(int exst) throws InvalidMfiDataException {

        ExstChunk subChunk = (ExstChunk) headerChunk.getSubChunks().get(ExstChunk.TYPE);
        if (subChunk != null) {
            subChunk.setExst(exst);
        } else {
            headerChunk.getSubChunks().put(ExstChunk.TYPE, new ExstChunk().init(exst));
        }
    }

    /**
     * Gets support information. ex. "P_Plugin 02.03.02"
     * <p>
     * this can be used for terminal type detection
     * </p>
     * @see SuptChunk
     */
    public String getSupt() {
        SuptChunk subChunk = (SuptChunk) headerChunk.getSubChunks().get(SuptChunk.TYPE);
        if (subChunk != null) {
            return subChunk.getSupt();
        } else {
            throw new NoSuchElementException(SuptChunk.TYPE);
        }
    }

    /**
     * Sets support information.
     *
     * TODO when creating mfi, add this for type detection
     *
     * @see SuptChunk
     */
    public void setSupt(String supt) throws InvalidMfiDataException {

        SuptChunk subChunk = (SuptChunk) headerChunk.getSubChunks().get(SuptChunk.TYPE);
        if (subChunk != null) {
            subChunk.setSupt(supt);
        } else {
            headerChunk.getSubChunks().put(SuptChunk.TYPE, new SuptChunk().init(supt));
        }
    }

    /**
     * Gets AudioDataChunk count.
     * @see AinfChunk
     * @since MFi 4.0
     */
    public int getAudioDataChunkCount() {
        AinfChunk subChunk = (AinfChunk) headerChunk.getSubChunks().get(AinfChunk.TYPE);
        if (subChunk != null) {
            return subChunk.getAudioChunksCount();
        } else {
            return 0;
        }
    }

    /**
     * Whether does it consist of AudioDataChunk only?
     * @see AinfChunk
     * @since MFi 4.0
     */
    public boolean isAudioDataOnly() {
        AinfChunk subChunk = (AinfChunk) headerChunk.getSubChunks().get(AinfChunk.TYPE);
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
        private static final ThreadLocal<DumpContext> dc = new ThreadLocal<>();

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
