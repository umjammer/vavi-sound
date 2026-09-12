/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.util.Map;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.MetaMessage;
import vavi.sound.smaf.Sequence;
import vavi.sound.smaf.SmafFileFormat;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.Track;
import vavi.sound.smaf.vavi.chunk.ChannelStatus;
import vavi.sound.smaf.vavi.chunk.Chunk;
import vavi.sound.smaf.vavi.chunk.ContentsInfoChunk;
import vavi.sound.smaf.vavi.chunk.FileChunk;
import vavi.sound.smaf.vavi.chunk.ScoreTrackChunk;
import vavi.sound.smaf.vavi.chunk.SequenceDataChunk;
import vavi.sound.smaf.vavi.chunk.TrackChunk.FormatType;
import vavi.sound.smaf.vavi.chunk.TrackChunk.SequenceType;
import vavi.sound.smaf.vavi.message.SmafContext;


/**
 * File Chunk.
 * <pre>
 * "MMMD"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class VaviSmafFileFormat extends SmafFileFormat {

    private static final Logger logger = System.getLogger(VaviSmafFileFormat.class.getName());

    /** {@value} */
    public static final String TYPE = "MMMD";

    /**
     * @see "vavi/sound/midi/package.html"
     */
    public static final int FILE_TYPE = 0x84;

    /** ringtone melody */
    private static final int CONTENTS_TYPE_MELODY = 0x01;

    /** */
    public VaviSmafFileFormat(int byteLength) {
        super(FILE_TYPE, byteLength);
    }

    // ----

    /** SMAF Sequence */
    private Sequence sequence;

    /**
     * @param sequence SMAF Sequence
     */
    VaviSmafFileFormat(Sequence sequence) {
        super(FILE_TYPE, 0); // TODO byteLength
        this.sequence = sequence;
    }

    /**
     * TODO byteLength
     *
     * @return SMAF Sequence
     */
    Sequence getSequence() {
        return sequence;
    }

    /** factory */
    static VaviSmafFileFormat readFrom(InputStream is) throws InvalidSmafDataException, IOException {
        try {
            Chunk chunk = Chunk.readFrom(is, null);
            if (chunk instanceof FileChunk fileChunk) {
                VaviSmafFileFormat sff = new VaviSmafFileFormat(fileChunk.getSize());
                sff.sequence = new SmafSequence(fileChunk);
                return sff;
            } else {
                throw new InvalidSmafDataException("stream is not smaf: first chunk: " + chunk.getId());
            }
        } catch (IOException | InvalidSmafDataException e) {
            throw e;
        } catch (Exception e) {
logger.log(Level.DEBUG, e.getMessage(), e);
            throw new InvalidSmafDataException(e);
        }
    }

    /**
     * Writes to the stream. Set the sequence in advance.
     * @after out has been {@link java.io.OutputStream#flush() flush}-ed
     * @throws IllegalStateException throws if no sequence is set
     */
    void writeTo(OutputStream out)
        throws InvalidSmafDataException, IOException {

        if (sequence == null) {
            throw new IllegalStateException("no sequence");
        }

        FileChunk fileChunk = toFileChunk(sequence);
        fileChunk.writeTo(out);
        out.flush();

        byteLength = fileChunk.getSize() + 8;
    }

    /**
     * Builds the chunks of a SMAF file out of a SMAF sequence, which is what
     * {@link SmafSequence} has taken apart when the sequence was read from a file.
     * <p>
     * Every {@link Track} becomes a Score Track Chunk, headed by the
     * {@link MetaEvent#META_MACHINE_DEPEND} message of the track when it has one, and the
     * {@link MetaEvent#META_NAME} and {@link MetaEvent#META_MARKER} messages of the first
     * track become the Option of the Contents Info Chunk.
     * </p>
     */
    private static FileChunk toFileChunk(Sequence sequence) throws InvalidSmafDataException {

        Track[] tracks = sequence.getTracks();
        if (tracks.length == 0) {
            throw new InvalidSmafDataException("no tracks");
        }

        FileChunk fileChunk = new FileChunk();
        fileChunk.setContentsInfoChunk(toContentsInfoChunk(tracks[0]));
        for (int t = 0; t < tracks.length; t++) {
            fileChunk.addScoreTrackChunk(toScoreTrackChunk(t, tracks[t]));
        }
        return fileChunk;
    }

    /** "CNTI", the chunk every SMAF file is required to have. */
    private static ContentsInfoChunk toContentsInfoChunk(Track track) {

        ContentsInfoChunk contentsInfoChunk = new ContentsInfoChunk();
        contentsInfoChunk.setContentsClass(ContentsInfoChunk.CONTENT_CLASS_YAMAHA);
        contentsInfoChunk.setContentsType(CONTENTS_TYPE_MELODY);
        contentsInfoChunk.setContentsCodeType(0x00);
        contentsInfoChunk.setCopyStatus(0x00);
        contentsInfoChunk.setCopyCounts(0x00);

        String songTitle = retrieveText(track, MetaEvent.META_NAME);
        if (songTitle != null) {
            contentsInfoChunk.addSubData("ST", songTitle);
        }
        String songWriter = retrieveText(track, MetaEvent.META_MARKER);
        if (songWriter != null) {
            contentsInfoChunk.addSubData("SW", songWriter);
        }

        return contentsInfoChunk;
    }

    /**
     * "MTR*", the score track and its Sequence Data Chunk.
     * <p>
     * Only a score track of {@link FormatType#HandyPhoneStandard} can be written, that is
     * what {@link vavi.sound.smaf.SmafMessage#getMessage()} encodes. A track without a header
     * (one which was not read from a file) is taken for one.
     * </p>
     *
     * @param trackNumber index into {@link Sequence#getTracks()}
     * @throws InvalidSmafDataException the track is not a writable score track
     */
    private static ScoreTrackChunk toScoreTrackChunk(int trackNumber, Track track) throws InvalidSmafDataException {

        ScoreTrackChunk scoreTrackChunk = new ScoreTrackChunk();
        // "MTR1" ~ "MTR4", the track numbers of the 4 tracks a HandyPhoneStandard file has
        scoreTrackChunk.setTrackNumber(trackNumber + 1);
        scoreTrackChunk.setFormatType(FormatType.HandyPhoneStandard);
        scoreTrackChunk.setSequenceType(SequenceType.StreamSequence);
        scoreTrackChunk.setDurationTimeBase(SmafContext.TIME_BASE);
        scoreTrackChunk.setGateTimeTimeBase(SmafContext.TIME_BASE);

        Map<String, Object> header = retrieveMachineDependentData(track);
        if (header != null) {
            if (header.get("localType") != ScoreTrackChunk.class) {
                throw new InvalidSmafDataException("not a score track: " + header.get("localType"));
            }
            if (header.get("formatType") instanceof FormatType formatType) {
                scoreTrackChunk.setFormatType(formatType);
            }
            if (header.get("sequenceType") instanceof SequenceType sequenceType) {
                scoreTrackChunk.setSequenceType(sequenceType);
            }
            if (header.get("durationTimeBase") instanceof Integer durationTimeBase) {
                scoreTrackChunk.setDurationTimeBase(durationTimeBase);
            }
            if (header.get("gateTimeTimeBase") instanceof Integer gateTimeTimeBase) {
                scoreTrackChunk.setGateTimeTimeBase(gateTimeTimeBase);
            }
            if (header.get("channelStatuses") instanceof ChannelStatus[] channelStatuses) {
                scoreTrackChunk.setChannelStatuses(channelStatuses);
            }
        }
        if (scoreTrackChunk.getFormatType() != FormatType.HandyPhoneStandard) {
            throw new InvalidSmafDataException("not writable format type: " + scoreTrackChunk.getFormatType());
        }

        SequenceDataChunk sequenceDataChunk = new SequenceDataChunk();
        for (int i = 0; i < track.size(); i++) {
            SmafMessage smafMessage = track.get(i).getMessage();
            if (!isIgnored(smafMessage)) {
                sequenceDataChunk.addSmafMessage(smafMessage);
            }
        }
        scoreTrackChunk.setSequenceDataChunk(sequenceDataChunk);
logger.log(Level.DEBUG, "track: " + trackNumber + ": " + sequenceDataChunk.getSmafMessages().size() + " messages");

        return scoreTrackChunk;
    }

    /** @return null when the track has no track header */
    private static Map<String, Object> retrieveMachineDependentData(Track track) {
        for (int i = 0; i < track.size(); i++) {
            if (track.get(i).getMessage() instanceof MetaMessage metaMessage &&
                metaMessage.getType() == MetaEvent.META_MACHINE_DEPEND.number()) {

                return metaMessage.getMapData();
            }
        }
        return null;
    }

    /** @return null when the track has no such meta message */
    private static String retrieveText(Track track, MetaEvent metaEvent) {
        for (int i = 0; i < track.size(); i++) {
            if (track.get(i).getMessage() instanceof MetaMessage metaMessage &&
                metaMessage.getType() == metaEvent.number() &&
                metaMessage.getData() != null) {

                return new String(metaMessage.getData(), 0, metaMessage.getLength(),
                                  Charset.forName(SmafSequence.writingEncoding));
            }
        }
        return null;
    }

    /**
     * Whether the message is not a part of the Sequence Data of a track. A meta message
     * carries the track header or the contents info of the file, those are written as the
     * Score Track Chunk and the Contents Info Chunk instead.
     */
    public static boolean isIgnored(SmafMessage message) {
        return message instanceof MetaMessage;
    }
}
