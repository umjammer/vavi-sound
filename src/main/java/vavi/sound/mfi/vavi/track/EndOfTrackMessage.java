/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.vavi.MfiContext;
import vavi.sound.mfi.vavi.MfiConvertible;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;


/**
 * EndOfTrackMessage.
 * <pre>
 *  0xff, 0xd# Play Control Information
 *  channel false
 *  delta   true
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 020627 nsano refine <br>
 *          0.11 030821 nsano implements {@link MidiConvertible} <br>
 *          0.12 030920 nsano repackage <br>
 *          0.13 031128 nsano implements {@link MfiConvertible} <br>
 */
public class EndOfTrackMessage extends ShortMessage
    implements MidiConvertible, MfiConvertible {

    /**
     *
     * @param delta delta time
     * @param data2 always 0
     */
    public EndOfTrackMessage(int delta, int data2) {
        super(delta, 0xff, 0xdf, data2);
    }

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 0xdf
     * @param data2 always 0
     */
    public EndOfTrackMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xdf, data2);
    }

    /** for {@link MfiConvertible} */
    public EndOfTrackMessage() {
        super(0, 0xff, 0xdf, 0);
    }

    /** */
    public String toString() {
        return "EndOfTrack:";
    }

    //----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) {
//        MetaMessage metaMessage = new MetaMessage();
//        metaMessage.setMessage(
//            0x27,            // End Of Track
//            new byte[] { 0x00 },
//            1);
//        return new MidiEvent[] {
//            new MidiEvent(metaMessage, context.getCurrent())
//        };

//logger.log(Level.DEBUG, "ignore: " + this);
        return null;
    }

    /**
     * @return The only return value of this method is the MfiEvent of EndOfTrackMessage for MFi tracks 0 ~ 3.
     *         Contains null if there is no track.
     */
    @Override
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context) {

        MfiEvent[] mfiEvents =  new MfiEvent[MfiContext.MAX_MFI_TRACKS];

//      float scale = context.getScale();

        for (int track = 0; track < MfiContext.MAX_MFI_TRACKS; track++) {
            if (context.isTrackUsed(track)) {
                long currentTick = midiEvent.getTick();
                int delta = context.retrieveAdjustedDelta(track, currentTick);

                EndOfTrackMessage mfiMessage = new EndOfTrackMessage();
                mfiMessage.setDelta(delta);

                mfiEvents[track] = new MfiEvent(mfiMessage, midiEvent.getTick());
            }
        }

        return mfiEvents;
    }
}
