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
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.SubMessage;
import vavi.sound.midi.MidiConstants.MetaEvent;


/**
 * MFi Header Sub Chunk for support information.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class SuptMessage extends SubMessage
    implements MidiConvertible {

    /** */
    public static final String TYPE = "supt";

    @Override
    public boolean accept(String key) {
        return TYPE.equals(key);
    }

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     *
     * @param type ignored
     * @return this
     */
    @Override
    public SubMessage init(String type, byte[] data) {
        return super.init(TYPE, data);
    }

    /** */
    public SubMessage init(String data) {
        return super.init(TYPE, data);
    }

    /** */
    public String getSupt() {
        try {
            return new String(getData(), readingEncoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    public void setSupt(String supt) throws InvalidMfiDataException {
        try {
            setData(supt.getBytes(readingEncoding));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        try {
            int length = getDataLength();
            byte[] data = getData();

            String string = new String(data, readingEncoding);
            return "supt: " + length + ": \"" + string + "\"";
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    // ----

    /** Meta 0x06 */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
            throws InvalidMidiDataException {

        MetaMessage metaMessage = new MetaMessage();

        metaMessage.setMessage(MetaEvent.META_MARKER.number(), // maker name
                getData(),
                getDataLength());

        return new MidiEvent[] {
                new MidiEvent(metaMessage, context.getCurrent())
        };
    }
}
