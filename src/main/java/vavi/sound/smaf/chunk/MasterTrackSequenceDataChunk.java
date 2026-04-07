/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.MetaMessage;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.message.EndOfSequenceMessage;
import vavi.sound.smaf.message.MachineDependentMessage.Factory;
import vavi.sound.smaf.message.UndefinedMessage;

import static java.lang.System.getLogger;


/**
 * MasterTrackSequenceData Chunk.
 * <pre>
 * "Mssq"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano initial version <br>
 */
public class MasterTrackSequenceDataChunk extends SequenceDataChunk {

    private static final Logger logger = getLogger(MasterTrackSequenceDataChunk.class.getName());

    private static final String FOURCC = "Mssq";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key);
    }

    @Override
    public MasterTrackSequenceDataChunk init(byte[] id, int size) {
        super.init(id, size);
logger.log(Level.DEBUG, "MasterTrackSequenceData: " + size + " bytes");
        return this;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        readHandyPhoneStandard(dis);

//byte[] data = new byte[size];
//dis.readFully(data);
//try {
//        readHandyPhoneStandard(new CrcDataInputStream(new ByteArrayInputStream(data), getId().getBytes(), size));
//} catch (Exception e) {
// logger.log(Level.WARNING, "parse error stage 1 Mssq#readHandyPhoneStandard: " + e);
// try {
//        super.readHandyPhoneStandard(new CrcDataInputStream(new ByteArrayInputStream(data), getId().getBytes(), size));
// } catch (Exception f) {
//  logger.log(Level.WARNING, "parse error stage 2 Mssq#readHandyPhoneStandard: " + f);
// }
//}
    }

    /** formatType 0 */
    @Override
    protected void readHandyPhoneStandard(CrcDataInputStream dis) throws IOException, InvalidSmafDataException {

        SmafMessage smafMessage = null;

        while (dis.available() > 0) {
            int duration = readVariableLength(dis);
            // event
            int e1 = dis.readUnsignedByte();
            if (e1 == 0xff) { // exclusive
                int e2 = dis.readUnsignedByte();
                switch (e2) {
                    case 0x2f: // meta end of track
                    case 0x51: // meta tempo
                    case 0x58: // meta time signature
                        int len = dis.readUnsignedByte();
                        byte[] b = new byte[len];
                        dis.readFully(b);
                        smafMessage = new MetaMessage();
                        ((MetaMessage) smafMessage).setMessage(e2, b, len);
                        logger.log(Level.WARNING, "meta 0xff, 0x%02x".formatted(e2));
                        break;
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
            } else if (e1 == 0x00) { // nop
                int e2 = dis.readUnsignedByte();
                if (e2 == 0x00) {
                    int e3 = dis.readUnsignedByte();
                    if (e3 == 0x00) {
                        smafMessage = new EndOfSequenceMessage(duration);
                    } else {
                        smafMessage = new UndefinedMessage(e1, e2, duration);
                        logger.log(Level.WARNING, "unknown 0x00, 0x00, 0x%02x".formatted(e3));
                    }
                }
            } else {
                smafMessage = new UndefinedMessage(e1, -1, 0);
                logger.log(Level.WARNING, "unhandled: %02x".formatted(e1));
            }

//            assert smafMessage == null : "smafMessage is null";
            messages.add(smafMessage);
        }
    }
}
