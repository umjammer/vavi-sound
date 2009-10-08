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
 * BackDropColorDefinitionMessage.
 * (Control Event 0x20..0x3f)
 * <pre>
 *  duration    1or2
 *  event type  0x20
 *  event size  1or2
 *  event data  event size ...
 * </pre>
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class BackDropColorDefinitionMessage extends ShortMessage {

    /** */
    private int backDropColor;

    /**
     * @param duration
     */
    public BackDropColorDefinitionMessage(int duration, byte[] data) {
        this.duration = duration;
        backDropColor = data[0] & 0xff;
    }

    /** */
    public String toString() {
        return "BackDropColorDefinition:" +
            " duration =" + duration +
            " backDropColor = " + backDropColor;
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
            baos.write(0x20);
            baos.write(1);
            baos.write(backDropColor);
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
