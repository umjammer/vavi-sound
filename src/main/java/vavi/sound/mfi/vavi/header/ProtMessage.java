/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

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
 * MFi Header Sub Chunk for data management and data protection information.
 * <pre>
 *  &quot;prot&quot; n bytes: data managing
 *  MIDI {@link MidiConstants.MetaEvent#META_TEXT_EVENT META_TEXT_EVENT (0x01)}
 * </pre>
 * <li> TODO use {@link CodeMessage}
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 *          0.01 030905 nsano implements {@link MfiConvertible} <br>
 */
public class ProtMessage extends SubMessage
    implements MidiConvertible, MfiConvertible {

    /** */
    public static final String TYPE = "prot";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public ProtMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /** for creator */
    public ProtMessage(String data) {
        super(TYPE, data);
    }

    /** for {@link MfiConvertible} */
    public ProtMessage() {
        super();
    }

    /** */
    public String getProt() {
        return new String(getData());
    }

    /** */
    public void setProt(String prot) throws InvalidMfiDataException {

        setData(prot.getBytes());
    }

    @Override
    public String toString() {
        return "prot: " + getDataLength() + ": " + getProt();
    }

    // ----

    /** Meta 0x01 */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        MetaMessage metaMessage = new MetaMessage();

        metaMessage.setMessage(MetaEvent.META_TEXT_EVENT.number(),
                               getData(),
                               getDataLength());

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrent())
        };
    }

    @Override
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException {

        MetaMessage metaMessage = (MetaMessage) midiEvent.getMessage();

        ProtMessage mfiMessage = new ProtMessage(MidiUtil.getDecodedMessage(metaMessage.getMessage()));

        return new MfiEvent[] {
            new MfiEvent(mfiMessage, midiEvent.getTick())
        };
    }
}
