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
 * GeneralPurposeDisplayMessage.
 * (Display Object Event 0x40�`0x7F)
 * <pre>
 *  duration    1or2
 *  event type  0x40�`0x7F
 *  duration    1or2
 * </pre>
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class GeneralPurposeDisplayMessage extends ShortMessage {

    /** */
    private int eventType;

    /** */
    private int lifeTime;

    /** */
    private int coordinates;

    /** */
    private int subBlocks;

    /**
     * @param duration
     */
    public GeneralPurposeDisplayMessage(int duration, int eventType, byte[] data) {
        this.duration = duration;
    }

    /** */
    public String toString() {
        return "GeneralPurposeDisplay:" +
            " duration=" + duration;
    }

    enum SubBlockType {
        Text,
        Bitmap,
        Image,
        Rectangle,
        TextBlock,
        ImageTile,
        BitmapTile;
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
                baos.write(eventType);
                writeOneToTwo(baos, 0); // TODO size
                writeOneToTwo(baos, lifeTime);
                writeOneToTwo(baos, coordinates);
                writeOneToTwo(baos, subBlocks);
            } catch (IOException e) {
                assert false;
            }
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
