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
import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.midi.MidiUtil;


/**
 * タイトル情報 MFi Header Sub Chunk.
 * <pre>
 *  &quot;titl&quot; n byte: mld title, &lt; 16 bytes expected, SJIS encoded
 *  MIDI {@link MidiConstants.MetaEvent#META_NAME META_NAME (0x03)}
 * </pre>
 * <li> TODO use {@link CodeMessage}
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class TitlMessage extends SubMessage
    implements MidiConvertible, MfiConvertible {

    /** */
    public static final String TYPE = "titl";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public TitlMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /** for creator */
    public TitlMessage(String data) {
        super(TYPE, data);
    }

    /** for {@link MfiConvertible} */
    public TitlMessage() {
        super();
    }

    /** */
    public String getTitle() {
        try {
            return new String(getData(), readingEncoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    public void setTitle(String title)
        throws InvalidMfiDataException {

        try {
            setData(title.getBytes(writingEncoding));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    public String toString() {
        return "titl: " + getDataLength() +
               ": \"" + getTitle() + "\"";
    }

    //----

    /** Meta 0x03 */
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        MetaMessage metaMessage = new MetaMessage();

        metaMessage.setMessage(MetaEvent.META_NAME.number(),    // シーケンス名/トラック名
                               getData(),
                               getDataLength());

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrent())
        };
    }

    /** */
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException {

        MetaMessage metaMessage = (MetaMessage) midiEvent.getMessage();

        TitlMessage mfiMessage = new TitlMessage(MidiUtil.getDecodedMessage(metaMessage.getMessage()));

        return new MfiEvent[] {
            new MfiEvent(mfiMessage, midiEvent.getTick())
        };
    }
}

/* */
