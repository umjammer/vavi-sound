/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.io.UnsupportedEncodingException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.smaf.SmafMessage;

import static java.lang.System.getLogger;


/**
 * VN Message.
 * TODO isn't MetaMessage enough?
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050412 nsano initial version <br>
 */
public class VNMessage extends SmafMessage
    implements MidiConvertible {

    private static final Logger logger = getLogger(VNMessage.class.getName());

    /** */
    private String vendorName;

    /**
     *
     * @param vendorName
     */
    public VNMessage(String vendorName) {
        this.vendorName = vendorName;
    }

    /** */
    public String getVendorName() {
        return vendorName;
    }

    /** */
    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    @Override
    public String toString() {
        return "VN:" +
            " vendorName=" + vendorName;
    }

    // ----

    @Override
    public byte[] getMessage() {
        return null; // TODO
    }

    @Override
    public int getLength() {
        return 0; // TODO
    }

    /**
     * @throws InvalidMidiDataException
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
        byte[] data;
        try {
            data = vendorName.getBytes("Windows-31J");
        } catch (UnsupportedEncodingException e) {
logger.log(Level.DEBUG, e);
            data = vendorName.getBytes();
        }
        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(MetaEvent.META_TEXT_EVENT.number(),
                               data,
                               data.length);
        return new MidiEvent[] {
            new MidiEvent(metaMessage, 0)
        };
    }
}
