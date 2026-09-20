/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sub;

import java.util.Date;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.SubChunk;
import vavi.sound.midi.MidiConstants.MetaEvent;


/**
 * MFi Header Sub Chunk for date information.
 *
 * <pre>
 *  &quot;date&quot; 8 bytes: date created
 *  format yyyymmdd (ex. 19990716)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class DateChunk extends SubChunk implements MidiConvertible {

    /** */
    public static final String TYPE = "date";

    @Override
    public boolean accept(String key) {
        return TYPE.equals(key);
    }

    /**
     * for {@link SubChunk#readFrom(java.io.InputStream)}
     * <li>TODO format, 8 byte check
     *
     * @param type ignored
     * @return this
     */
    @Override
    public SubChunk init(String type, byte[] data) {
        return super.init(TYPE, data);
    }

    /** TODO format, 8 byte check */
    public SubChunk init(Date date) {
        return super.init(TYPE, date.toString());
    }

    @Override
    public String toString() {
        int length = getDataLength();
        byte[] data = getData();

        return "date: " + length + ": " + new String(data);
    }

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
}
