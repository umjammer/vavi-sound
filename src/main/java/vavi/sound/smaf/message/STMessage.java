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

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.smaf.SmafMessage;
import vavi.util.Debug;


/**
 * ST Message.
 * TODO isn't MetaMessage enough?
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050412 nsano initial version <br>
 */
public class STMessage extends SmafMessage
    implements MidiConvertible {

    /** */
    private String songTitle;

    /**
     *
     * @param songTitle
     */
    public STMessage(String songTitle) {
        this.songTitle = songTitle;
    }

    /** */
    public String getSongTitle() {
        return songTitle;
    }

    /** */
    public void setSongTitle(String songTitle) {
        this.songTitle = songTitle;
    }

    /** */
    public String toString() {
        return "ST:" +
            " songTitle=" + songTitle;
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
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
        byte[] data;
        try {
            data = songTitle.getBytes("Windows-31J");
        } catch (UnsupportedEncodingException e) {
Debug.println(e);
            data = songTitle.getBytes();
        }
        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(MetaEvent.META_NAME.number(),
                               data,
                               data.length);
        return new MidiEvent[] {
            new MidiEvent(metaMessage, 0)
        };
    }
}
