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
 * UserMessage.
 * (Control Event 0x20..0x3f)
 * <pre>
 *  duration    1or2
 *  event type  0x22
 *  event size  1or2
 *  event data  event size ...
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class UserMessage extends ShortMessage {

    /** */
    private final int userEventId;

    /**
     * @param duration
     * @param data 0: userEventId 0x0 ~ 0xf
     */
    public UserMessage(int duration, byte[] data) {
        this.duration = duration;
        this.userEventId = data[0] & 0x0f;
    }

    /** */
    public String toString() {
        return "User:" +
        " duration=" + duration +
        " userEventId=" + userEventId;
    }

    //----

    /* */
    @Override
    public byte[] getMessage() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FormatType formatType = FormatType.HandyPhoneStandard; // TODO
        switch (formatType) {
        case HandyPhoneStandard:
            try {
                MidiUtil.writeVarInt(new DataOutputStream(baos), duration);
            } catch (IOException e) {
                assert false;
            }
            baos.write(0x22);
            baos.write(1);
            baos.write(userEventId);
            break;
        case MobileStandard_Compress:
        case MobileStandard_NoCompress:
        default:
            throw new UnsupportedOperationException("not specified");
        }
        return baos.toByteArray();
    }

    /* */
    @Override
    public int getLength() {
        return getMessage().length;
    }
}
