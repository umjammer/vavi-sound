/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.InputStream;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;
import vavi.sound.smaf.message.UndefinedMessage;
import vavi.sound.smaf.message.graphics.BackDropColorDefinitionMessage;
import vavi.sound.smaf.message.graphics.GeneralPurposeDisplayMessage;
import vavi.sound.smaf.message.graphics.NopMessage;
import vavi.sound.smaf.message.graphics.OffsetOriginMessage;
import vavi.sound.smaf.message.graphics.ResetOrigneMessage;
import vavi.sound.smaf.message.graphics.UserMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * GraphicsTrackSequenceData Chunk.
 * <pre>
 * "Gsq*"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class GraphicsTrackSequenceDataChunk extends SequenceDataChunk {

    /** */
    private int sequenceNumber;

    /** */
    public GraphicsTrackSequenceDataChunk(byte[] id, int size) {
        super(id, size);

        this.sequenceNumber = id[3];
Debug.println("GraphicsTrackSequenceData[" + sequenceNumber + "]: " + size + " bytes");
    }

    /** */
    public GraphicsTrackSequenceDataChunk() {
        System.arraycopy("Gsq".getBytes(), 0, id, 0, 3);
        this.size = 0;
    }

    /** */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {

        FormatType formatType = ((TrackChunk) parent).getFormatType();
        switch (formatType) {
        case HandyPhoneStandard:
            readHandyPhoneStandard(is);
            break;
        default:
            throw new InvalidSmafDataException("FormatType: " + formatType);
        }
Debug.println("messages: " + messages.size());
    }

    /** formatType 0 */
    protected void readHandyPhoneStandard(InputStream is)
        throws InvalidSmafDataException, IOException {

        SmafMessage smafMessage = null;

        while (available() > 0) {
            // -------- duration --------
            int duration = readOneToTwo(is);
//Debug.println("duration: " + duration + ", 0x" + StringUtil.toHex4(duration));
            // -------- event --------
            int e1 = read(is);
            switch (e1) {
            case 0x00: // short event
                smafMessage = new NopMessage(duration);
                break;
            case 0x01: // short event
                smafMessage = new ResetOrigneMessage(duration);
                break;
            case 0x20: { // control event
                int size = readOneToTwo(is);
                byte[] data = new byte[size];
                read(is, data);
                smafMessage = new BackDropColorDefinitionMessage(duration, data);
              } break;
            case 0x21: { // control event
                int size = readOneToTwo(is);
                byte[] data = new byte[size];
                read(is, data);
                smafMessage = new OffsetOriginMessage(duration, data);
              } break;
            case 0x22: { // control event
                int size = readOneToTwo(is);
                byte[] data = new byte[size];
                read(is, data);
                smafMessage = new UserMessage(duration, data);
              } break;
            case 0x40: { // display object event
                int size = readOneToTwo(is);
                byte[] data = new byte[size];
                read(is, data);
                smafMessage = new GeneralPurposeDisplayMessage(duration, e1, data);
              } break;
            default: {
                int size = readOneToTwo(is);
                byte[] data = new byte[size];
                read(is, data);
                smafMessage = new UndefinedMessage(duration);
Debug.println("reserved: " + StringUtil.toHex2(e1));
              } break;
            }

//Debug.println(available() + ", " + smafMessage);
Debug.println("message: " + smafMessage);
            messages.add(smafMessage);
        }
    }
}

/* */
