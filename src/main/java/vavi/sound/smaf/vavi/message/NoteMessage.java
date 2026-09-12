/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.NoSuchElementException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafMessage;

import static java.lang.System.getLogger;


/**
 * NoteMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class NoteMessage extends SmafMessage
    implements MidiConvertible, SmafConvertible {

    private static final Logger logger = getLogger(NoteMessage.class.getName());

    /** note */
    private int note;

    /** smaf channel */
    private int channel;

    /** note length (!= 0) */
    private int gateTime;

    /** 0 ~ 127 */
    private int velocity;

    /**
     * Octave
     * <pre>
     * 01 Low
     * 00 Mid Low
     * 11 Mid High
     * 10 High
     * </pre>
     */
    private int octave = -1;

    /**
     * Creates a note message for HandyPhoneStandard.
     *
     * @param duration
     * @param status <pre>
     *  76 54 3210
     *  ~~ ~~ ~~~~
     *  |  |  +- note 0x1 ~ 0xc
     *  |  +- octave 0 ~ 3
     *  +- channel 0 ~ 3
     * </pre>
     * @param gateTime    note length (!= 0)
     */
    public NoteMessage(int duration, int status, int gateTime) {
        this.duration =  duration;
        this.channel  = (status & 0xc0) >> 6;
        this.octave   = (status & 0x30) >> 4;
        this.note     =  status & 0x0f;
        this.gateTime =  gateTime;
//if (gateTime == 0) {
// logger.log(Level.WARNING, "★★★★★ gateTime == 0: " + channel + "ch, note: " + note);
//}
        this.velocity = -1;
    }

    /**
     * for Mobile Standard (w/o velocity)
     */
    public NoteMessage(int duration, int channel, int note, int gateTime) {
        this.duration = duration;
        this.channel  = channel;
        this.note     = note;
        this.gateTime = gateTime;
        this.velocity = -1;
    }

    /**
     * for Mobile Standard
     * @param velocity 0 ~ 127
     */
    public NoteMessage(int duration, int channel, int note, int gateTime, int velocity) {
        this(duration, channel, note, gateTime);
        this.velocity = velocity;
    }

    /** for SmafConvertible, a HandyPhoneStandard note without velocity */
    public NoteMessage() {
        this.octave = 0;
        this.velocity = -1;
    }

    /**
     * Gets a note.
     * @return note
     */
    public int getNote() {
        return switch (octave) {
            case 0 -> note;      // 00
            case 1 -> note + 12; // 01
            case 2 -> note + 24; // 10
            case 3 -> note + 36; // 11
            default -> note;
        };
    }

    /**
     * Sets a note.
     * @param note SMAF note
     */
    public void setNote(int note) {
        if (octave != -1) {
            if (note > 36) {
                this.octave = 3;
                this.note = note - 36;
            } else if (note > 24) {
                this.octave = 2;
                this.note = note - 24;
            } else if (note > 12) {
                this.octave = 1;
                this.note = note - 12;
            } else {
                this.octave = 0;
                this.note = note;
            }
        } else {
            this.note = note;
        }
    }

    /**
     * Gets voice number.
     * @return voice number
     */
    public int getChannel() {
        return channel;
    }

    /**
     * Sets voice number.
     * @param channel voce number
     */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /**
     * Gets note length.
     * @return note length
     */
    public int getGateTime() {
        return gateTime;
    }

    /**
     * Sets note length.
     * @param gateTime note length
     */
    public void setGateTime(int gateTime) {
        this.gateTime = gateTime;
    }

    /**
     * @return Returns the octave.
     */
    public int getOctave() {
        return octave;
    }

    /**
     * @return Returns the velocity.
     */
    public int getVelocity() {
        return velocity;
    }

    @Override
    public String toString() {
        return "Note:" +
            " duration=" + duration +
            " channel=" + channel +
            " note=%02x".formatted(getNote())  +
            " gateTime=%04x".formatted(gateTime) +
                (velocity == -1 ? "" : " velocity=%04x".formatted(velocity));
    }

    @Override
    public byte[] getMessage() {
        return HandyPhoneStandard.note(duration, (channel << 6) | (octave << 4) | note, gateTime);
    }

    @Override
    public int getLength() {
        return getMessage().length;
    }

private static int uc = 0;

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

if (gateTime == 0) {
 if (uc < 10) {
  logger.log(Level.WARNING, "★★★★★ gateTime == 0 ignored: " + this);
 }
 uc++;
 return null;
}
        int length = (int) context.getTickOfGateTime(this.gateTime);
        int pitch = context.retrievePitch(this.channel, getNote());
        int midiChannel = context.retrieveChannel(this.channel);
        int velocity = this.velocity == -1 ? context.getVelocity(this.channel) : context.setVelocity(this.channel, this.velocity);

        MidiEvent[] events = new MidiEvent[2];
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.NOTE_ON,
                                midiChannel,
                                pitch,
                                velocity);
//logger.log(Level.TRACE, "note: " + channel + ": " + pitch);
        events[0] = new MidiEvent(shortMessage, context.getCurrentTick());

        shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.NOTE_OFF,
                                midiChannel,
                                pitch,
                                0);
        events[1] = new MidiEvent(shortMessage, context.getCurrentTick() + length);

        return events;
    }

    @Override
    public boolean accept(String key) {
        return "short.128".equals(key) || "short.144".equals(key);
    }

    /**
     * The NoteOff of the pair is consumed here, so only the NoteOn becomes a message.
     *
     * TODO if the Δ time is smaller than the gate time of the previous NoteMessage of the same voice and same key,
     *      it will be the continuation sound from the previous NoteMessage.
     * TODO if there is room for the next note, do you extend it, and if there is not, cut it? (unimplemented)
     */
    @Override
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context)
        throws InvalidSmafDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int data1 = shortMessage.getData1();
//logger.log(Level.TRACE, midiEvent.getTick() + ", " + channel + ", " + command + ", " + context.retrievePitch(channel, data1));

        if (SmafContext.isNoteOff(shortMessage)) {
            if (!context.isNoteOffEventUsed()) {
logger.log(Level.DEBUG, "[" + context.getMidiEventIndex() + "] no pair of ON for: " + channel + "ch, " + data1);
            }

            return null;
        }

        MidiEvent noteOffEvent;

        try {
            noteOffEvent = context.getNoteOffMidiEvent();
        } catch (NoSuchElementException e) {
logger.log(Level.WARNING, "[" + context.getMidiEventIndex() + "] no pair of OFF for: " + channel + "ch, " + data1);
            return null;
        }

        int track = context.retrieveSmafTrack(channel);
        int voice = context.retrieveVoice(channel);

        int length = (int) (context.retrieveSteps(noteOffEvent.getTick()) - context.retrieveSteps(midiEvent.getTick()));
        // a gate time of 0 is no note at all, and one message cannot be longer than maxSteps
        int gateTime = Math.clamp(length, 1, HandyPhoneStandard.maxSteps);
        int duration = context.getDuration(track);

        context.setBeforeTick(track, midiEvent.getTick());

        NoteMessage smafMessage = new NoteMessage();
        smafMessage.setDuration(duration);
        smafMessage.setChannel(voice);
        smafMessage.setNote(context.retrievePitch(channel, data1));
        smafMessage.setGateTime(gateTime);
//logger.log(Level.TRACE, channel + "ch, " + smafMessage);

        if (context.isPercussion(channel)) {
            // a rhythm channel has no pitch, the program change before the note selects the drum sound
            smafMessage.setDuration(0);
            return new SmafEvent[] {
                new SmafEvent(new ProgramChangeMessage(duration, voice, data1), midiEvent.getTick()),
                new SmafEvent(smafMessage, midiEvent.getTick())
            };
        }

        return new SmafEvent[] {
            new SmafEvent(smafMessage, midiEvent.getTick())
        };
    }
}
