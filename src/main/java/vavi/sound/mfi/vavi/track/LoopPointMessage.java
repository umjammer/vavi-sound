/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.util.logging.Level;
import javax.sound.midi.MidiEvent;
import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.util.Debug;


/**
 * LoopPointMessage.
 * <pre>
 *  0xff, 0xd# 演奏管理情報
 *  channel false
 *  delta   ?
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 030821 nsano implements {@link MidiConvertible} <br>
 *          0.02 030920 nsano repackage <br>
 */
public class LoopPointMessage extends ShortMessage
    implements MidiConvertible {

    /** 0 ~ 3 */
    private int nest;
    /** loop times, 01111b means forever */
    private int times;
    /** 00b: start, 01b: end */
    private int start;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 0xdd
     * @param data2
     * <pre>
     *  76 5432 10
     *  ~~ ~~~~ ~~
     *  |  |    +- start/end
     *  |  +- times
     *  +- nest
     * </pre>
     */
    public LoopPointMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xdd, data2);

        this.nest  = (data2 & 0xc0) >> 6;
        this.times = (data2 & 0x3c) >> 2;
        if (times == 0x0f) {
            times = -1;
        }
        this.start =  data2 & 0x03;
    }

    /** ループID */
    public int getNest() {
        return nest;
    }

    /** ループ回数 0: 無限回 */
    public int getTimes() {
        return times;
    }

    /** */
    public boolean isStart() {
        return start == 0;
    }

    /** */
    public String toString() {
        return "LoopPoint:" +
               " nest="  + nest  +
               " times=" + times +
               " start=" + start;
    }

    //----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) {
Debug.println(Level.INFO, "ignore: " + this);
        return null;
    }
}
