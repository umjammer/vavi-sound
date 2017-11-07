/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.SysexMessage;
import vavi.sound.smaf.message.UndefinedMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * SetupDataChunk Chunk.
 * <pre>
 * "[MA]tsu"
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041228 nsano initial version <br>
 */
public class SetupDataChunk extends Chunk {

    /** */
    public SetupDataChunk(byte[] id, int size) {
        super(id, size);
Debug.println("SetupData: " + size + " bytes");
    }

    /** */
    public SetupDataChunk() {
        System.arraycopy("tsu".getBytes(), 0, id, 1, 3);
        this.size = 0;
    }

    /** */
    protected void init(InputStream is, Chunk parent) throws InvalidSmafDataException, IOException {

        ScoreTrackChunk.FormatType formatType = ((ScoreTrackChunk) parent).getFormatType();
        switch (formatType) {
        case HandyPhoneStandard:
            readHandyPhoneStandard(is);
            break;
        case MobileStandard_Compress:
            readMobileStandard(is); // TODO Huffman
            break;
        case MobileStandard_NoCompress:
        case Unknown3:
            readMobileStandard(is);
            break;
        }
Debug.println("messages: " + messages.size());
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
    private void readHandyPhoneStandard(InputStream is) throws InvalidSmafDataException, IOException {

        SmafMessage smafMessage = null;

        while (available() > 0) {
            // -------- event --------
            int e1 = read(is);
            if (e1 == 0xff) { // exclusive
                int e2 = read(is);
                switch (e2) {
                case 0xf0:
                    int messageSize = read(is);
                    byte[] data = new byte[messageSize];
                    read(is, data);
                    // TODO end check 0xf7
                    smafMessage = SysexMessage.Factory.getSysexMessage(0, data);
                    break;
                default:
                    smafMessage = new UndefinedMessage(0);
Debug.println(Level.WARNING, "unknown 0xff, 0x" + StringUtil.toHex2(e2));
                    break;
                }
            } else {
                smafMessage = new UndefinedMessage(0);
Debug.println(Level.WARNING, "unhandled: " + StringUtil.toHex2(e1));
            }

            if (smafMessage != null) {
                messages.add(smafMessage);
            } else {
                assert false : "smafMessage is null";
            }
        }
    }

    /** formatType 1, 2 */
    private void readMobileStandard(InputStream is) throws InvalidSmafDataException, IOException {

        SmafMessage smafMessage = null;

        while (available() > 0) {
            // event
            int status = read(is);
            if (status == 0xf0) { // exclusive
                int messageSize = readOneToFour(is);
                byte[] data = new byte[messageSize];
                read(is, data);
                // TODO end check 0xf7
                smafMessage = SysexMessage.Factory.getSysexMessage(0, data);
            } else {
                smafMessage = new UndefinedMessage(0);
Debug.println(Level.WARNING, "unhandled: " + StringUtil.toHex2(status));
            }

            if (smafMessage != null) {
                messages.add(smafMessage);
            } else {
                assert false : "smafMessage is null";
            }
        }
    }

    /** TODO formatType */
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

/* */
