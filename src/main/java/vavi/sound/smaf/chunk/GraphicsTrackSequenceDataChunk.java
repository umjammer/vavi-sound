/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Level;

import vavi.sound.midi.MidiUtil;
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
Debug.println(Level.FINE, "GraphicsTrackSequenceData[" + sequenceNumber + "]: " + size + " bytes");
    }

    /** */
    public GraphicsTrackSequenceDataChunk() {
        System.arraycopy("Gsq".getBytes(), 0, id, 0, 3);
        this.size = 0;
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

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

    /** formatType 0 */
    protected void readHandyPhoneStandard(DataInputStream dis)
        throws InvalidSmafDataException, IOException {

        SmafMessage smafMessage;

        while (dis.available() > 0) {
            // -------- duration --------
            int duration = MidiUtil.readVariableLength(dis);
//Debug.println("duration: " + duration + ", 0x" + StringUtil.toHex4(duration));
            // -------- event --------
            int e1 = dis.readUnsignedByte();
            switch (e1) {
            case 0x00: // short event
                smafMessage = new NopMessage(duration);
                break;
            case 0x01: // short event
                smafMessage = new ResetOrigneMessage(duration);
                break;
            case 0x20: { // control event
                int size = MidiUtil.readVariableLength(dis);
                byte[] data = new byte[size];
                dis.readFully(data);
                smafMessage = new BackDropColorDefinitionMessage(duration, data);
              } break;
            case 0x21: { // control event
                int size = MidiUtil.readVariableLength(dis);
                byte[] data = new byte[size];
                dis.readFully(data);
                smafMessage = new OffsetOriginMessage(duration, data);
              } break;
            case 0x22: { // control event
                int size = MidiUtil.readVariableLength(dis);
                byte[] data = new byte[size];
                dis.readFully(data);
                smafMessage = new UserMessage(duration, data);
              } break;
            case 0x40: { // display object event
                int size = MidiUtil.readVariableLength(dis);
                byte[] data = new byte[size];
                dis.readFully(data);
                smafMessage = new GeneralPurposeDisplayMessage(duration, e1, data);
              } break;
            default: {
                int size = MidiUtil.readVariableLength(dis);
                byte[] data = new byte[size];
                dis.readFully(data);
                smafMessage = new UndefinedMessage(duration);
Debug.printf(Level.FINE, "reserved: %02x\n", e1);
              } break;
            }

//Debug.println(available() + ", " + smafMessage);
Debug.println(Level.FINE, "message: " + smafMessage);
            messages.add(smafMessage);
        }
    }
}

/* */
