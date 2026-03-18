/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage;
import vavi.sound.midi.MidiConstants.MetaEvent;


/**
 * CuePointMessage.
 * <pre>
 *  0xff, 0xd# Play Control Information
 *  channel false
 *  delta   ?
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 020627 nsano refine <br>
 *          0.11 030821 nsano implements {@link MidiConvertible} <br>
 *          0.12 030920 nsano repackage <br>
 */
public class CuePointMessage extends ShortMessage
    implements MidiConvertible, TrackMessage {

    /** 00: start, 01: end */
    private boolean start;

    @Override
    public boolean accept(String key) {
        return "255.b.208".equals(key);
    }

    /**
     * 0xd0
     *
     * @param delta delta time
     * @param data2 00: start, 01: end
     */
    public CuePointMessage init(int delta, int data2) {
        return this.init(delta, 0xff, 0xd0, data2);
    }

    /**
     * for {@link TrackChunk}
     * @param delta delta time
     * @param status
     * @param data1 always 0xd0
     * @param data2 00: start, 01: end
     */
    @Override
    public CuePointMessage init(int delta, int status, int data1, int data2) {
        super.init(delta, 0xff, 0xd0, data2);

        this.start = (data2 == 0);

        return this;
    }

    /** */
    public boolean isStart() {
        return start;
    }

    @Override
    public String toString() {
        return "CuePoint:" + " start=" + start;
    }

    // ----

    /**
     * TODO {@link javax.sound.midi.MetaMessage} has 0x07 cue point
     * @return return nothing
     */
    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {

//      if (start) {
//          byte[] data = new byte[6];

//          data[0] = (byte) 0xf0;
//          data[1] = (byte) 0x7e; // ID number
//          data[2] = (byte) 0x7f; // device ID
//          data[3] = (byte) 0x09; // sub-ID#1, 0x09 General MIDI
//          data[4] = (byte) 0x01; // sub-ID#2, 0x01 General MIDI System On
//          data[5] = (byte) 0xf7;

//          SysexMessage sysexMessage = new SysexMessage();
//          sysexMessage.setMessage(data, data.length);
//          return new MidiEvent[] {
//               new MidiEvent(sysexMessage, context.getCurrent())
//          };
//      } else {
//logger.log(Level.TRACE, "ignore: " + this);
//        return null;
//      }
        MetaMessage metaMessage = new MetaMessage();

        String text = start ? "start" : "stop";

        metaMessage.setMessage(MetaEvent.META_QUE_POINT.number(), // cue point
                               text.getBytes(), text.getBytes().length);

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrent())
        };
    }
}
