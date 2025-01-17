/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message.graphics;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.ShortMessage;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;


/**
 * GeneralPurposeDisplayMessage.
 * (Display Object Event 0x40 ~ 0x7F)
 * <pre>
 *  duration    1or2
 *  event type  0x40 ~ 0x7F
 *  duration    1or2
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
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

    @Override
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
        BitmapTile
    }

    // ----

    @Override
    public byte[] getMessage() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FormatType formatType = FormatType.HandyPhoneStandard; // TODO
        switch (formatType) {
        case HandyPhoneStandard:
            try {
                DataOutputStream dos = new DataOutputStream(baos);
                MidiUtil.writeVarInt(dos, duration);
                baos.write(eventType);
                MidiUtil.writeVarInt(dos, 0); // TODO size
                MidiUtil.writeVarInt(dos, lifeTime);
                MidiUtil.writeVarInt(dos, coordinates);
                MidiUtil.writeVarInt(dos, subBlocks);
            } catch (IOException e) {
                assert false;
            }
            break;
        case MobileStandard_Compress:
        case MobileStandard_NoCompress:
        default:
            throw new UnsupportedOperationException("not specified");
        }
        return baos.toByteArray();
    }

    @Override
    public int getLength() {
        return getMessage().length;
    }
}
