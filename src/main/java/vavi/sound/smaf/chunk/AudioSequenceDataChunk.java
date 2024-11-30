/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;
import vavi.sound.smaf.message.WaveMessage;

import static java.lang.System.getLogger;


/**
 * Audio SequenceData Chunk.
 * <pre>
 * "Atsq"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071009 nsano initial version <br>
 */
public class AudioSequenceDataChunk extends SequenceDataChunk {

    private static final Logger logger = getLogger(AudioSequenceDataChunk.class.getName());

    /** */
    public AudioSequenceDataChunk(byte[] id, int size) {
        super(id, size);
logger.log(Level.DEBUG, "AudioSequenceData: " + size + " bytes");
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
//logger.log(Level.TRACE, "available: " + is.available() + ", " + available());
//skip(is, size); // TODO
        FormatType formatType = ((TrackChunk) parent).getFormatType();
        switch (formatType) {
        case HandyPhoneStandard:
            readHandyPhoneStandard(dis);
            break;
        default:
            throw new InvalidSmafDataException("FormatType: " + formatType);
        }
logger.log(Level.DEBUG, "messages: " + messages.size());
    }

    /**
     * internal use
     * for Atsq
     */
    @Override
    protected SmafMessage getHandyPhoneStandardMessage(int duration, int data, int gateTime) {
        return new WaveMessage(duration, data, gateTime);
    }
}
