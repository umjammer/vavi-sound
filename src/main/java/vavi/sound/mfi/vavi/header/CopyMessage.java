/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import java.io.UnsupportedEncodingException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.vavi.SubMessage;
import vavi.sound.mfi.vavi.MfiContext;
import vavi.sound.mfi.vavi.MfiConvertible;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.midi.MidiConstants;
import vavi.sound.midi.MidiUtil;


/**
 * Copyright èÓïÒ MFi Header Sub Chunk.
 * <pre>
 *  &quot;copy&quot; n bytes: copyright
 *  MIDI {@link MidiConstants#META_COPYRIGHT META_COPYRIGHT (0x02)}
 * </pre>
 * <li> TODO use {@link CodeMessage} 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class CopyMessage extends SubMessage implements MidiConvertible, MfiConvertible {

    /** */
    public static final String TYPE = "copy";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public CopyMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /** for creator */
    public CopyMessage(String data) {
        super(TYPE, data);
    }

    /** for {@link MfiConvertible} */
    public CopyMessage() {
        super();
    }

    /** */
    public String toString() {
        try {
            int length = getDataLength();
            byte[] data = getData();

            return "copy: " + length + ": " + new String(data, readingEncoding);
        } catch (UnsupportedEncodingException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
    }

    // ----

    /** Meta 0x02 */
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {

        MetaMessage metaMessage = new MetaMessage();

        metaMessage.setMessage(MidiConstants.META_COPYRIGHT, // íòçÏå†ï\é¶
                               getData(), getDataLength());

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrent())
        };
    }

    /** */
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context) throws InvalidMfiDataException {

        MetaMessage metaMessage = (MetaMessage) midiEvent.getMessage();

        CopyMessage mfiMessage = new CopyMessage(MidiUtil.getDecodedMessage(metaMessage.getMessage()));

        return new MfiEvent[] {
            new MfiEvent(mfiMessage, midiEvent.getTick())
        };
    }
}

/* */
