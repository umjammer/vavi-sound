/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.message.MachineDependentMessage.Factory;
import vavi.sound.smaf.message.UndefinedMessage;

import static java.lang.System.getLogger;
import static vavi.sound.smaf.chunk.Chunk.DumpContext.getDC;


/**
 * SetupDataChunk Chunk.
 * <pre>
 * "[MA]tsu"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041228 nsano initial version <br>
 */
public class SetupDataChunk extends Chunk {

    private static final Logger logger = getLogger(SetupDataChunk.class.getName());

    private static final String FOURCC = "[MA]tsu";

    @Override
    protected boolean accept(String key) {
        return Pattern.compile(FOURCC).matcher(key).matches();
    }

    @Override
    public SetupDataChunk init(byte[] id, int size) {
        super.init(id, size);
logger.log(Level.DEBUG, "SetupData: " + size + " bytes");
        return this;
    }

    /** */
    public SetupDataChunk() {
        System.arraycopy(FOURCC.getBytes(), 0, id, 1, 3);
        this.size = 0;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {

        ScoreTrackChunk.FormatType formatType = ((TrackChunk) parent).getFormatType();
        switch (formatType) {
        case HandyPhoneStandard:
            readHandyPhoneStandard(dis);
            break;
        case MobileStandard_Compress:
            readMobileStandard(dis); // TODO Huffman
            break;
        case MobileStandard_NoCompress:
        case SEQU:
            readMobileStandard(dis);
            break;
        }
logger.log(Level.DEBUG, "messages: " + messages.size());
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
    private void readHandyPhoneStandard(CrcDataInputStream dis) throws InvalidSmafDataException, IOException {

        SmafMessage smafMessage;

        while (dis.available() > 0) {
            // event
            int e1 = dis.readUnsignedByte();
            if (e1 == 0xff) { // exclusive
                int e2 = dis.readUnsignedByte();
                switch (e2) {
                case 0xf0:
                    int messageSize = dis.readUnsignedByte();
                    byte[] data = new byte[messageSize];
                    dis.readFully(data);
                    smafMessage = Factory.getSysexMessage(0, e2, data, messageSize);
                    break;
                default:
                    smafMessage = new UndefinedMessage(e1, e2, 0);
logger.log(Level.WARNING, "unknown 0xff, 0x%02x".formatted(e2));
                    break;
                }
            } else {
                smafMessage = new UndefinedMessage(e1, -1, 0);
logger.log(Level.WARNING, "unhandled: %02x".formatted(e1));
            }

//            assert smafMessage == null : "smafMessage is null";
            messages.add(smafMessage);
        }
    }

    /** formatType 1, 2 */
    private void readMobileStandard(CrcDataInputStream dis) throws InvalidSmafDataException, IOException {

        SmafMessage smafMessage;

        while (dis.available() > 0) {
            // event
            int status = dis.readUnsignedByte();
            if (status == 0xf0) { // exclusive
                int messageSize = MidiUtil.readVariableLength(dis);
                byte[] data = new byte[messageSize];
                dis.readFully(data);
                smafMessage = Factory.getSysexMessage(0, status, data, messageSize);
            } else {
                smafMessage = new UndefinedMessage(status, -1, 0);
logger.log(Level.WARNING, "unhandled: %02x".formatted(status));
            }

//            assert smafMessage == null : "smafMessage is null";
            messages.add(smafMessage);
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
    private final List<SmafMessage> messages = new ArrayList<>();

    /**
     * @return Returns the messages.
     */
    public List<SmafMessage> getSmafMessages() {
        return messages;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString());
        try (var dc = getDC().open()) {
            messages.stream().map(m -> dc.format(m.toString())).forEach(sb::append);
        }

        return sb.toString();
    }
}
