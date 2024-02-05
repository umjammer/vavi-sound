/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.SysexMessage;
import vavi.sound.smaf.message.UndefinedMessage;
import vavi.util.Debug;


/**
 * SetupDataChunk Chunk.
 * <pre>
 * "[MA]tsu"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041228 nsano initial version <br>
 */
public class SetupDataChunk extends Chunk {

    /** */
    public SetupDataChunk(byte[] id, int size) {
        super(id, size);
Debug.println(Level.FINE, "SetupData: " + size + " bytes");
    }

    /** */
    public SetupDataChunk() {
        System.arraycopy("tsu".getBytes(), 0, id, 1, 3);
        this.size = 0;
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {

        ScoreTrackChunk.FormatType formatType = ((ScoreTrackChunk) parent).getFormatType();
        switch (formatType) {
        case HandyPhoneStandard:
            readHandyPhoneStandard(dis);
            break;
        case MobileStandard_Compress:
            readMobileStandard(dis); // TODO Huffman
            break;
        case MobileStandard_NoCompress:
        case Unknown3:
            readMobileStandard(dis);
            break;
        }
Debug.println(Level.FINE, "messages: " + messages.size());
    }

    /**
     * formatType 0
     *
     * <pre>
     *  ff
     *  f0
     *  ##   length
     *  ...  data
     *
     * </pre>
     */
    private void readHandyPhoneStandard(MyDataInputStream dis) throws InvalidSmafDataException, IOException {

        SmafMessage smafMessage;

        while (dis.available() > 0) {
            // -------- event --------
            int e1 = dis.readUnsignedByte();
            if (e1 == 0xff) { // exclusive
                int e2 = dis.readUnsignedByte();
                switch (e2) {
                case 0xf0:
                    int messageSize = dis.readUnsignedByte();
                    byte[] data = new byte[messageSize];
                    dis.readFully(data);
                    // TODO end check 0xf7
                    smafMessage = SysexMessage.Factory.getSysexMessage(0, data);
                    break;
                default:
                    smafMessage = new UndefinedMessage(0);
Debug.printf(Level.WARNING, "unknown 0xff, 0x%02x\n", e2);
                    break;
                }
            } else {
                smafMessage = new UndefinedMessage(0);
Debug.printf(Level.WARNING, "unhandled: %02x\n", e1);
            }

            if (smafMessage != null) {
                messages.add(smafMessage);
            } else {
                assert false : "smafMessage is null";
            }
        }
    }

    /** formatType 1, 2 */
    private void readMobileStandard(MyDataInputStream dis) throws InvalidSmafDataException, IOException {

        SmafMessage smafMessage;

        while (dis.available() > 0) {
            // event
            int status = dis.readUnsignedByte();
            if (status == 0xf0) { // exclusive
                int messageSize = MidiUtil.readVariableLength(dis);
                byte[] data = new byte[messageSize];
                dis.readFully(data);
                // TODO end check 0xf7
                smafMessage = SysexMessage.Factory.getSysexMessage(0, data);
            } else {
                smafMessage = new UndefinedMessage(0);
Debug.printf(Level.WARNING, "unhandled: %02x\n", status);
            }

            if (smafMessage != null) {
                messages.add(smafMessage);
            } else {
                assert false : "smafMessage is null";
            }
        }
    }

    /** TODO formatType */
    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        for (SmafMessage message : messages) {
            os.write(message.getMessage());
        }
    }

    /** SysEx messages */
    private List<SmafMessage> messages = new ArrayList<>();

    /**
     * @return Returns the messages.
     */
    public List<SmafMessage> getSmafMessages() {
        return messages;
    }
}
