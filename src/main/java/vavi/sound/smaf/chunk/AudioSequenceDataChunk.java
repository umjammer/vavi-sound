/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;
import vavi.sound.smaf.message.WaveMessage;
import vavi.util.Debug;


/**
 * Audio SequenceData Chunk.
 * <pre>
 * "Atsq"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071009 nsano initial version <br>
 */
public class AudioSequenceDataChunk extends SequenceDataChunk {

    /** */
    public AudioSequenceDataChunk(byte[] id, int size) {
        super(id, size);
Debug.println(Level.FINE, "AudioSequenceData: " + size + " bytes");
    }

    /** */
    public AudioSequenceDataChunk() {
        System.arraycopy("Atsq".getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    /** TODO how to get formatType from parent chunk ??? */
    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {
//Debug.println("available: " + is.available() + ", " + available());
//skip(is, size); // TODO
        FormatType formatType = ((TrackChunk) parent).getFormatType();
        switch (formatType) {
        case HandyPhoneStandard:
            readHandyPhoneStandard(dis);
            break;
        default:
            throw new InvalidSmafDataException("FormatType: " + formatType);
        }
Debug.println(Level.FINE, "messages: " + messages.size());
    }

    /**
     * internal use
     * Atsq の場合
     */
    @Override
    protected SmafMessage getHandyPhoneStandardMessage(int duration, int data, int gateTime) {
        return new WaveMessage(duration, data, gateTime);
    }
}

/* */
