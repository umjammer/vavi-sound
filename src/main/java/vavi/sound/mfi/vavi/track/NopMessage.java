/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage;


/**
 * NopMessage.
 * <pre>
 *  0xff, 0xd# Play Control Information
 *  channel false
 *  delta   true
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 030821 nsano implements {@link MidiConvertible} <br>
 *          0.02 030920 nsano repackage <br>
 */
public class NopMessage extends ShortMessage
    implements MidiConvertible, TrackMessage {

    @Override
    public boolean accept(String key) {
        return "255.b.222".equals(key);
    }

    /**
     * 0xde
     *
     * @param delta delta time
     * @param data2 always 0
     */
    public NopMessage init(int delta, int data2) {
        return (NopMessage) super.init(delta, 0xff, 0xde, data2);
//logger.log(Level.TRACE, "NOP: delta: " + delta);
    }

    /**
     * for {@link TrackChunk}
     * @param delta delta time
     * @param status
     * @param data1 always 0xde
     * @param data2 always 0
     */
    @Override
    public NopMessage init(int delta, int status, int data1, int data2) {
        return (NopMessage) super.init(delta, 0xff, 0xde, data2);
//logger.log(Level.TRACE, "NOP: delta: " + delta);
    }

    @Override
    public String toString() {
        return "Nop: delta=" + (data[3] & 0xff);
    }

    // ----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) {
//logger.log(Level.TRACE, "ignore: " + this);
        return null;
    }
}
