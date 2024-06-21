/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.NoSuchElementException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.NoteMessage;
import vavi.sound.midi.MidiUtil;

import static java.lang.System.getLogger;


/**
 * VaviNoteMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030821 nsano initial version <br>
 *          0.01 030826 nsano implements {@link MidiConvertible} <br>
 */
public class VaviNoteMessage extends NoteMessage
    implements MidiConvertible, MfiConvertible {

    private static final Logger logger = getLogger(VaviNoteMessage.class.getName());

    /** MFi */
    public VaviNoteMessage(int delta, int status, int data) {
        super(delta, status, data);
    }

    /** case of MFi2 note = 1 */
    public VaviNoteMessage(int delta, int status, int data1, int data2) {
        super(delta, status, data1, data2);
    }

    /** for {@link MfiConvertible}, note = 1 */
    public VaviNoteMessage() { // TODO public mhh...
        super();
    }

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int voice    = getVoice();
        int velocity = getVelocity() * 2;
        int length   = getGateTime();
        int channel  = voice + 4 * context.getMfiTrackNumber();
        int pitch    = getNote();

        channel = context.retrieveChannel(channel);
        pitch = context.retrievePitch(channel, pitch);

        MidiEvent[] events = new MidiEvent[2];
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.NOTE_ON,
                                channel,
                                pitch,
                                velocity);
//logger.log(Level.DEBUG, "note: " + channel + ": " + pitch);
        events[0] = new MidiEvent(shortMessage, context.getCurrent());

        shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.NOTE_OFF,
                                channel,
                                pitch,
                                0);
        events[1] = new MidiEvent(shortMessage, context.getCurrent() + length);

        return events;
    }

    /**
     * TODO if the Δ time is smaller than the gate time of the previous NoteMessage of the same voice and same key,
     *      it will be the continuation sound from the previous NoteMessage.
     * TODO if there is room for the next note, do you extend it, and if there is not, cut it? (unimplemented)
     */
    @Override
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int command = shortMessage.getCommand();
        int data1 = shortMessage.getData1();
        int data2 = shortMessage.getData2();
//logger.log(Level.DEBUG, midiEvent.getTick() + ", " + channel + ", " + command + ", " + (context.retrievePitch(channel, data1) + 45) + ", " + (data2 / 2));

        if (command == ShortMessage.NOTE_OFF ||
            // note on with velocity 0
            (command == ShortMessage.NOTE_ON && data2 == 0)) {

            if (!context.isNoteOffEventUsed()) {
logger.log(Level.INFO, "[" + context.getMidiEventIndex() + "] no pair of ON for: " + channel + "ch, " + data1);
            }

            return null;
        } else /* if (command == ShortMessage.NOTE_ON) */ {

            MidiEvent noteOffEvent;

            try {
                noteOffEvent = context.getNoteOffMidiEvent();
            } catch (NoSuchElementException e) {
logger.log(Level.WARNING, "[" + context.getMidiEventIndex() + "] no pair of OFF for: " + channel + "ch, " + data1);
                return null;
            }

            int track = context.retrieveMfiTrack(channel);
            int voice = context.retrieveVoice(channel);

            double scale = context.getScale();

            long currentTick = midiEvent.getTick();
            long noteOffTick = noteOffEvent.getTick();
            int length = (int) Math.round((noteOffTick - currentTick) / scale);
//if (length >= 255) {
// try {
//  MidiEvent nextMidiEvent = context.getNextMidiEvent();
//  long nextTick = nextMidiEvent.getTick();
//  int nextDelta = Math.round((nextTick - currentTick) /scale);
//  if (length <= nextDelta) {
//   logger.log(Level.DEBUG, channel + "ch, " + data1 + " len(all): " + length + ", next: " + nextDelta);
//  } else {
//   logger.log(Level.DEBUG, channel + "ch, " + data1 + " len(cut): " + length + ", next: " + nextDelta);
//   length = nextDelta;
//  }
// } catch (NoSuchElementException e) {
//  logger.log(Level.DEBUG, channel + "ch, " + data1 + " len(last): " + length);
// }
//}
if (length == 0) {
// if ((noteOffTick - currentTick) / scale > 0f) {
  logger.log(Level.WARNING, "length is 0 ~ 1, " + MidiUtil.paramString(shortMessage) + ", " + ((noteOffTick - currentTick) / scale));
//  length = 1;
// } else {
//  logger.log(Level.WARNING, "length is 0, " + MidiUtil.paramString(shortMessage) + ", " + ((noteOffTick - currentTick) / scale));
// }
} else if (length < 0) {
 logger.log(Level.WARNING, "length < 0, " + MidiUtil.paramString(shortMessage) + ", " + ((noteOffTick - currentTick) / scale));
}
            int delta = context.getDelta(context.retrieveMfiTrack(channel));

            int onLength = (length + 254) / 255;
            MfiEvent[] mfiEvents = new MfiEvent[1/* onLength */];
            for (int i = 0; i < Math.max(onLength, 1); i++) {

                NoteMessage mfiMessage = new VaviNoteMessage();
                mfiMessage.setDelta(i == 0 ? delta : 0);
                mfiMessage.setVoice(voice);
                mfiMessage.setNote(context.retrievePitch(channel, data1));
                mfiMessage.setGateTime(i == onLength - 1 ? length % 255 : 255);
                mfiMessage.setVelocity(data2 / 2);
if (length >= 255) {
 logger.log(Level.INFO, channel + "ch, " + mfiMessage.getNote() + ", " + mfiMessage.getDelta() + ":[" + i + "]:" + (i == onLength - 1 ? length % 255 : 255) + "/" + length);
}
//logger.log(Level.DEBUG, channel + ", " + mfiMessage.getVoice() + ", " + ((mfiMessage.getMessage()[1] & 0xc0) >> 6));
                mfiEvents[i] = new MfiEvent(mfiMessage, 0L); // TODO 0l
//if (mfiEvents[i] == null) {
// logger.log(Level.DEBUG, "[" + i + "]: " + mfiEvents[i]);
//}

                if (i == 0) {
                    context.setPreviousTick(track, midiEvent.getTick());
                    break;
                } else {
//                  context.incrementBeforeTick(track, i == onLength - 1 ? length % 255 : 255);
                }
            }

            return mfiEvents;
        }
    }
}
