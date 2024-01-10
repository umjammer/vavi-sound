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


/**
 * NopMessage.
 * <pre>
 *  0xff, 0xd# 演奏管理情報
 *  channel false
 *  delta   true
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 030821 nsano implements {@link MidiConvertible} <br>
 *          0.02 030920 nsano repackage <br>
 */
public class NopMessage extends ShortMessage
    implements MidiConvertible {

    /**
     * 0xde
     *
     * @param delta delta time
     * @param data2 always 0
     */
    public NopMessage(int delta, int data2) {
        super(delta, 0xff, 0xde, data2);
//Debug.println("NOP: delta: " + delta);
    }

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 always 0xde
     * @param data2 always 0
     */
    public NopMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xde, data2);
//Debug.println("NOP: delta: " + delta);
    }

    /** */
    public String toString() {
        return "Nop: delta=" + (data[3] & 0xff);
    }

    //----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) {
//Debug.println("ignore: " + this);
        return null;
    }
}

/* */
