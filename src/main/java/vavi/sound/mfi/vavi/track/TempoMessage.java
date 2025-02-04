/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.vavi.MfiContext;
import vavi.sound.mfi.vavi.MfiConvertible;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;

import static java.lang.System.getLogger;


/**
 * Represents tempo message.
 * <pre>
 *  0xff, 0xc#
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 020627 nsano refine <br/>
 *          0.11 030821 nsano implements {@link MidiConvertible} <br/>
 *          0.12 030920 nsano repackage <br/>
 *          0.13 031128 nsano refine <br/>
 */
public class TempoMessage extends ShortMessage
    implements MidiConvertible, MfiConvertible {

    private static final Logger logger = getLogger(TempoMessage.class.getName());

    /** quarter note resolution @see #timeBaseTable */
    private int timeBase;
    /** quarter note count per minute 20 ~ 125 ~ 255 */
    private int tempo;

    /** quarter note resolutions @index 0xc0 ~ 0xcf */
    private static final int[] timeBaseTable = {
        6, 12, 24, 48, 96, 192, 384, -1,
        15, 30, 60, 120, 240, 480, 960, -1
    };

    /**
     * Creates a tempo message.
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status 0xff fixed
     * @param data1 0xc0 ~ 0xcf index of time base
     * @param data2 20 ~ 255 tempo
     */
    public TempoMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, data1, data2);

        this.timeBase = timeBaseTable[data1 & 0x0f];
        this.tempo    = Math.max(data2, 20);
    }

    /** for {@link MfiConvertible} */
    public TempoMessage() {
        super(0, 0xff, 0xc3, 125);

        this.timeBase = 48;
        this.tempo    = 125;
    }

    /** */
    public int getTimeBase() {
        return timeBase;
    }

    /** */
    public void setTimeBase(int timeBase) {
        this.timeBase = timeBase;
        // TODO accepts -1
        for (int i = 0; i < timeBaseTable.length; i++) {
            if (timeBase == timeBaseTable[i]) {
if (timeBase < 0) {
 logger.log(Level.WARNING, "timeBase < 0: " + timeBase);
}
                this.data[2] = (byte) (0xc0 | i);
                return;
            }
        }
        throw new IllegalArgumentException("timeBase = " + timeBase);
    }

    /** */
    public int getTempo() {
        return tempo;
    }

    /** */
    public void setTempo(int tempo) {
        this.tempo = tempo < 20 ? 20 : tempo & 0xff;
        this.data[3] = (byte) this.tempo;
    }

    @Override
    public String toString() {
        return "Tempo:" +
            " timeBase=" + timeBase +
            " tempo="    + tempo;
    }

    // ----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int tempo    = getTempo();
        int timeBase = getTimeBase();
        // quarter note length in μsec TODO is round OK?, TODO 48??? (actually 60 * 10^6 / tempo)
        int l = (int) Math.round(60d * 1000000d / ((48d / timeBase) * tempo));
//logger.log(Level.TRACE, this);
//logger.log(Level.TRACE, "%d = %02x, %02x, %02x".formatted(l, (l / 0x10000) & 0xff, ((l % 0x10000) / 0x100) & 0xff, l % 0x100) & 0xff));
        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(
            0x51,
            new byte[] {
                    (byte)  ((l / 0x10000) & 0xff),
                    (byte) (((l % 0x10000) / 0x100) & 0xff),
                    (byte)  ((l % 0x100)   & 0xff)
            },
            3);
        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrent())
        };
    }

    @Override
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException {

        MetaMessage metaMessage = (MetaMessage) midiEvent.getMessage();
//        int type = metaMessage.getType();
        byte[] data = metaMessage.getData();
//logger.log(Level.TRACE, "data.length: " + data.length);

        int timeBase = getNearestTimeBase(context.getTimeBase());
        int l = ((data[0] & 0xff) << 16) |
                ((data[1] & 0xff) <<  8) |
                 (data[2] & 0xff);
//logger.log(Level.TRACE, "(CALC) timeBase: " + timeBase + ", tempo: " + tempo + ", l; " + l);

        // TODO no more change if scale is changed once?
        if (context.isScaleChanged()) {
            timeBase = getNearestTimeBase((int) (context.getTimeBase() / context.getScale()));
            tempo = (int) Math.round(60d * 1000000d / ((48d / timeBase) * l));
//logger.log(Level.TRACE, "(SET) tempo > " + MAX_SCALELESS + ": timeBase: " + timeBase + ", tempo: " + tempo);
        } else {
            timeBase = getNearestTimeBase(context.getTimeBase());
            tempo = (int) Math.round(60d * 1000000d / ((48d / timeBase) * l));
//logger.log(Level.TRACE, "(SET) timeBase: " + timeBase + ", tempo: " + tempo);
        }

        TempoMessage mfiMessage = new TempoMessage();
        mfiMessage.setDelta(context.getDelta(0)); // TODO ???
        mfiMessage.setTimeBase(timeBase);
        mfiMessage.setTempo(tempo);

        context.setPreviousTick(0, midiEvent.getTick());

        return new MfiEvent[] {
            new MfiEvent(mfiMessage, midiEvent.getTick())
        };
    }

    /** for sorting */
    private record Pair(int index, int value) {}

    /** */
    public static int getNearestTimeBase(int timeBase) {
        List<Pair> table = new ArrayList<>();
        for (int i = 0; i < timeBaseTable.length; i++) {
            if (timeBaseTable[i] != -1) {
                int difference = Math.abs(timeBase - timeBaseTable[i]);
                table.add(new Pair(i, difference));
            } else {
                table.add(new Pair(i, 10000));
            }
        }

        table.sort((p1, p2) -> {
            int v1 = p1.value;
            int v2 = p2.value;
            return v1 - v2;
        });
//for (int i = 0; i < table.size(); i++) {
// logger.log(Level.DEBUG, "(" + i + ") timeBaseTable[" + table.get(i).index + "], " + table.get(i).value);
//}
//logger.log(Level.TRACE, "(CHANGE) " + timeBase + " -> " + timeBaseTable[table.get(0).index]);
        return timeBaseTable[table.get(0).index];
    }
}
