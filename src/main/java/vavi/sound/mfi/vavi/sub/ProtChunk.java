/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sub;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.vavi.MfiContext;
import vavi.sound.mfi.vavi.MfiConvertible;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.SubChunk;
import vavi.sound.midi.MidiConstants;
import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.midi.MidiUtil;


/**
 * MFi Header Sub Chunk for data management and data protection information.
 * <pre>
 *  &quot;prot&quot; n bytes: data managing
 *  MIDI {@link MidiConstants.MetaEvent#META_TEXT_EVENT META_TEXT_EVENT (0x01)}
 * </pre>
 * TODO use {@link CodeChunk} as charset
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 *          0.01 030905 nsano implements {@link MfiConvertible} <br>
 */
public class ProtChunk extends SubChunk
    implements MidiConvertible, MfiConvertible {

    /** */
    public static final String TYPE = "prot";

    @Override
    public boolean accept(String key) {
        return "meta.1".equals(key) || TYPE.equals(key);
    }

    /**
     * for {@link SubChunk#readFrom(java.io.InputStream)}
     *
     * @param type ignored
     * @return this
     */
    @Override
    public ProtChunk init(String type, byte[] data) {
        return (ProtChunk) super.init(TYPE, data);
    }

    /** for creator */
    public ProtChunk init(String data) {
        return (ProtChunk) super.init(TYPE, data);
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
                concat((TYPE + ": ").getBytes(), getData()),
                4 + 2 + getDataLength());

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrent())
        };
    }

    @Override
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException {

        MetaMessage metaMessage = (MetaMessage) midiEvent.getMessage();

        ProtChunk mfiMessage = new ProtChunk().init(MidiUtil.getDecodedMessage(metaMessage.getMessage()));

        return new MfiEvent[] {
            new MfiEvent(mfiMessage, midiEvent.getTick())
        };
    }
}
