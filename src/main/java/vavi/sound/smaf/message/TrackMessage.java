/*
 * Copyright (c) 2008 by KLab Inc., All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafFileFormat;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.Track;
import vavi.util.Debug;


/**
 * TrackMessage. TODO 要るのか？
 *
 * @author <a href="mailto:sano-n@klab.org">Naohide Sano</a> (nsano)
 * @version 0.00 080415 nsano initial version <br>
 */
public class TrackMessage {

    /** */
    private int trackNumber;

    /** */
    private Track track;

    /** */
    public TrackMessage(int trackNumber, Track track) {
        this.trackNumber = trackNumber;
        this.track = track;
    }

    /** */
    public void writeTo(OutputStream out) throws IOException {

        DataOutputStream dos = new DataOutputStream(out);

        dos.writeBytes(getType());
        dos.writeInt(getDataLength());
Debug.println("track: " + trackNumber + ": " + getDataLength());
        for (int j = 0; j < track.size(); j++) {
            SmafEvent event = track.get(j);
            SmafMessage message = event.getMessage();
            if (!SmafFileFormat.isIgnored(message)) {
                byte[] data = message.getMessage();
                dos.write(data, 0, data.length);
            }
        }
    }

    /**
     * track の中身を見て ScoreTrack, PcmAudio, Graphic に振り分け
     */
    private String getType() {
        return "ATR\0"; // TODO
    }

    /** 
     * 書き出し用
     * {@link Track}[0] の不必要なデータは省かれます。
     */
    public int getDataLength() {
        int trackLength = 0;

        for (int j = 0; j < track.size(); j++) {
            SmafEvent event = track.get(j);
            SmafMessage message = event.getMessage();
            if (!SmafFileFormat.isIgnored(message)) {
                trackLength += message.getLength();
            }
        }

        return trackLength;
    }
}

/* */
