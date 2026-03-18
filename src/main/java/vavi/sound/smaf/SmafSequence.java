/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.util.List;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.smaf.chunk.Chunk;
import vavi.sound.smaf.chunk.FileChunk;
import vavi.sound.smaf.chunk.TrackChunk;

import static java.lang.System.getLogger;


/**
 * SmafSequence.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071012 nsano initial version <br>
 */
class SmafSequence extends Sequence {

    private static final Logger logger = getLogger(SmafSequence.class.getName());

    /** TODO use encoding in header info */
    private static final String writingEncoding;

    /** TODO content should be moved to SmafFileFormat/FileChunk */
    SmafSequence(FileChunk fileChunk) throws InvalidSmafDataException {
        for (TrackChunk scoreTrackChunk : fileChunk.getScoreTrackChunks()) {
            createTrack(scoreTrackChunk.getSmafEvents());
logger.log(Level.DEBUG, "ScoreTrack: " + getTracks()[getTracks().length - 1].size());
        }
        for (TrackChunk pcmAudioTrackChunk : fileChunk.getPcmAudioTrackChunks()) {
            createTrack(pcmAudioTrackChunk.getSmafEvents());
logger.log(Level.DEBUG, "PcmAudioTrack: " + getTracks()[getTracks().length - 1].size());
        }
        for (TrackChunk graphicsTrackChunk : fileChunk.getGraphicsTrackChunks()) {
            createTrack(graphicsTrackChunk.getSmafEvents());
logger.log(Level.DEBUG, "GraphicsTrack: " + getTracks()[getTracks().length - 1].size());
        }
        if (fileChunk.getMasterTrackChunk() != null) {
            createTrack(fileChunk.getMasterTrackChunk().getSmafEvents());
logger.log(Level.DEBUG, "MasterTrack: " + getTracks()[getTracks().length - 1].size());
        }
        if (fileChunk.getMMMGChunk() != null) {
            for (int i = 0; i < fileChunk.getMMMGChunk().getTracks(); i++) {
                fileChunk.getMMMGChunk().setCurrentTrack(i);
                createTrack(fileChunk.getMMMGChunk().getSmafEvents());
logger.log(Level.DEBUG, "events: " + getTracks()[getTracks().length - 1].size());
            }
        }
        // SMF XF Information | SMAF Contents Info Chunk
        // -------------------+-------------------------
        // song title         | ST: song title
        // song writer        | SW: song writer
        // words writer       | WW: words writer
        // arrangement writer | AW: arrangement writer
        // artist name        | AN: artist name
        if (tracks.isEmpty()) throw new InvalidSmafDataException("no tracks");
logger.log(Level.DEBUG, "tracks: " + tracks.size() + ", " + tracks.get(0).size());
        Track track0 = tracks.get(0);
        String title = "title";
        String prot = "vavi";
        if (fileChunk.getOptionalDataChunk() != null) {
            for (Chunk dataChunk : fileChunk.getOptionalDataChunk().getDataChunks()) {
logger.log(Level.DEBUG, dataChunk);
                // TODO
            }
        } else if (fileChunk.getContentsInfoChunk() != null) { // TODO no need for if as it is required
            title = fileChunk.getContentsInfoChunk().getSubDataByTag("ST");
            prot = fileChunk.getContentsInfoChunk().getSubDataByTag("SW");
            // TODO create meta for ContentsInfoChunk
        }
        MetaMessage metaMessage = new MetaMessage();
        byte[] b = (prot != null ? prot : "").getBytes(Charset.forName(writingEncoding));
        metaMessage.setMessage(MetaEvent.META_MARKER.number(), b, b.length);
        insert(track0, new SmafEvent(metaMessage, 0), 0); // vn vendor name
        metaMessage = new MetaMessage();
        b = (title != null ? title : "").getBytes(Charset.forName(writingEncoding));
        metaMessage.setMessage(MetaEvent.META_NAME.number(), b, b.length);
        insert(track0, new SmafEvent(metaMessage, 0), 0); // st song title
    }

    /** */
    private void createTrack(List<SmafEvent> events) {
        Track track = createTrack();
        if (events != null) {
            for (SmafEvent event : events) {
                track.add(event);
            }
        }
    }

    /**
     * hack of Track
     */
    private static void insert(Track track, SmafEvent event, int index) {
        try {
            Class<? extends Track> clazz = track.getClass();
//for (Field field : clazz.getDeclaredFields()) {
// logger.log(Level.DEBUG, "field: " + field);
//}
            @SuppressWarnings("unchecked")
            List<SmafEvent> events = (List<SmafEvent>) clazz.getDeclaredField("events").get(track);
            events.add(index, event);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static {
        // encodings
        writingEncoding = System.getProperty("vavi.sound.smaf.encoding.write", "Windows-31J");
        logger.log(Level.DEBUG, "write encoding: " + writingEncoding);
    }
}
