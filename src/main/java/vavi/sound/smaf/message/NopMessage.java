/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.sound.midi.MidiEvent;

import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.ShortMessage;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;


/**
 * NopMessage.
 * <pre>
 *  duration    1or2
 *              0xff
 *              0x00
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class NopMessage extends ShortMessage
    implements MidiConvertible {

    /** TODO formatType */
    public static final int maxSteps = 16511;

    /**
     * @param duration
     */
    public NopMessage(int duration) {
        this.duration = duration;
    }

    /** */
    public String toString() {
        return "Nop:" +
            " duration=" + duration +
            " (" + String.format("%04x", duration) + ")";
    }

    // ----

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
            baos.write(0xff);
            baos.write(0x00);
            break;
        case MobileStandard_Compress:
        case MobileStandard_NoCompress:
        default:
            throw new UnsupportedOperationException("not implemented"); // TODO
//            break;
        }
        return baos.toByteArray();
    }

    /* */
    @Override
    public int getLength() {
        return getMessage().length;
    }

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) {
        return null;
    }
}
