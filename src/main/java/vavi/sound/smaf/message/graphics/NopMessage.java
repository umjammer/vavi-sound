/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message.graphics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import vavi.sound.smaf.ShortMessage;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;


/**
 * NopMessage.
 * (Short Control Event 0x00..0x1f)
 * TODO ShortMessage ではない気がする
 * <pre>
 *  duration    1or2
 *  event type  0x00
 * </pre>
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class NopMessage extends ShortMessage {

    /**
     * @param duration
     */
    public NopMessage(int duration) {
        this.duration = duration;
    }

    /** */
    public String toString() {
        return "Nop:" +
            " duration=" + duration;
    }

    //----

    /* */
    public byte[] getMessage() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FormatType formatType = FormatType.HandyPhoneStandard; // TODO
        switch (formatType) {
        case HandyPhoneStandard:
            try {
                writeOneToTwo(baos, duration);
            } catch (IOException e) {
                assert false;
            }
            baos.write(0x00);
            break;
        case MobileStandard_Compress:
        case MobileStandard_NoCompress:
            throw new UnsupportedOperationException("not specified");
        }
        return baos.toByteArray();
    }

    /* */
    public int getLength() {
        return getMessage().length;
    }
}

/* */
