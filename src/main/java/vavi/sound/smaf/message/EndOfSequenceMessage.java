/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.midi.MidiEvent;

import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;
import vavi.util.Debug;


/**
 * EndOfSequenceMessage.
 * <pre>
 *  duration    1or2 maybe 0x00
 *  data0       0x00
 *  data1       0x00
 *  data2       0x00
 * </pre>
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class EndOfSequenceMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible, SmafConvertible {

    /** for SmafConvertible */
    public EndOfSequenceMessage() {
    }

    /** @param duration always 0 ??? */
    public EndOfSequenceMessage(int duration) {
        this.duration = duration;
    }

    /** */
    public String toString() {
        return "EOS: duration=" + duration;
    }

    //----

    /* */
    public byte[] getMessage() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FormatType formatType = FormatType.HandyPhoneStandard; // TODO
        switch (formatType) {
        case HandyPhoneStandard:
            try {
                writeOneToTwo(baos, duration);
            } catch (IOException e) {
                assert false;
            }
            baos.write(0x00);
            baos.write(0x00);
            baos.write(0x00);
            break;
        case MobileStandard_Compress:
        case MobileStandard_NoCompress:
            throw new UnsupportedOperationException("not implemented"); // TODO
//            break;
        }
        return baos.toByteArray();
    }

    /* */
    public int getLength() {
        return getMessage().length;
    }

    /** NOP 等の対策で EOT の tick をカウントしたものに設定する。 */
    public MidiEvent[] getMidiEvents(MidiContext context) {
        javax.sound.midi.Track midiTrack = context.getMidiTrack();
        MidiEvent midiEvent = midiTrack.get(midiTrack.size() - 1); // should be EoT
        midiEvent.setTick(context.getCurrentTick());
Debug.println("EOT: " + midiEvent.getMessage().getClass().getName());

        return null;
    }

    /**
     * @return このメソッドの戻り値のみ SMAF トラック 0 ~ 3 の EndOfSequenceMessage の
     * SmafEvent になる。トラックがない場合は null が入っている
     */
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context) {

        SmafEvent[] smafEvents = new SmafEvent[SmafContext.MAX_SMAF_TRACKS];

        for (int track = 0; track < SmafContext.MAX_SMAF_TRACKS; track++) {
            if (context.isTrackUsed(track)) {
                long currentTick = midiEvent.getTick();
                int delta = context.retrieveAdjustedDelta(track, currentTick);

                EndOfSequenceMessage smafMessage = new EndOfSequenceMessage();
                smafMessage.setDuration(delta);

                smafEvents[track] = new SmafEvent(smafMessage, midiEvent.getTick());
            }
        }

        return smafEvents;
    }
}

/* */
