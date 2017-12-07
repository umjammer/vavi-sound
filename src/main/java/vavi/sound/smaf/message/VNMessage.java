/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.io.UnsupportedEncodingException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.midi.MidiConstants;
import vavi.sound.smaf.SmafMessage;
import vavi.util.Debug;


/**
 * VN Message.
 * TODO MetaMessage じゃだめなの？
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050412 nsano initial version <br>
 */
public class VNMessage extends SmafMessage
    implements MidiConvertible {

    /** */
    private String venderName;

    /**
     *
     * @param venderName
     */
    public VNMessage(String venderName) {
        this.venderName = venderName;
    }

    /** */
    public String getVenderName() {
        return venderName;
    }

    /** */
    public void setVenderName(String venderName) {
        this.venderName = venderName;
    }

    /** */
    public String toString() {
        return "VN:" +
            " venderName=" + venderName;
    }

    //----

    /* */
    @Override
    public byte[] getMessage() {
        return null; // TODO
    }

    /* */
    @Override
    public int getLength() {
        return 0;   // TODO
    }

    /**
     * @throws InvalidMidiDataException
     */
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
        byte[] data = null;
        try {
            data = venderName.getBytes("Windows-31J");
        } catch (UnsupportedEncodingException e) {
Debug.println(e);
            data = venderName.getBytes();
        }
        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(MidiConstants.META_TEXT_EVENT,
                               data,
                               data.length);
        return new MidiEvent[] {
            new MidiEvent(metaMessage, 0)
        };
    }
}

/* */
