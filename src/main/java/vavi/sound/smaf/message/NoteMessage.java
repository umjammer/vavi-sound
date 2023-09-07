/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.util.NoSuchElementException;
import java.util.logging.Level;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafMessage;
import vavi.util.Debug;


/**
 * NoteMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class NoteMessage extends SmafMessage
    implements MidiConvertible {

    /** 音階 */
    private int note;

    /** smaf channel */
    private int channel;

    /** 音長 (!= 0) */
    private int gateTime;

    /** 0 ~ 127 */
    private int velocity;

    /**
     * オクターブ
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
// Debug.println(Level.WARNING, "★★★★★ gateTime == 0: " + channel + "ch, note: " + note);
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

    /** */
    protected NoteMessage() {
    }

    /**
     * 音階を取得します．
     * @return 音階
     */
    public int getNote() {
        switch (octave) {
        case 0:            // 00
            return note;
        case 1:         // 01
            return note + 12;
        case 2:            // 10
            return note + 24;
        case 3:         // 11
            return note + 36;
        default:
            return note;
        }
    }

    /**
     * 音階を設定します．
     * @param note SMAF の音階
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
     * ボイスナンバを取得します．
     * @return ボイスナンバ
     */
    public int getChannel() {
        return channel;
    }

    /**
     * ボイスナンバを設定します．
     * @param channel ボイスナンバ
     */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /**
     * 音長を取得します．
     * @return 音長
     */
    public int getGateTime() {
        return gateTime;
    }

    /**
     * 音長を設定します．
     * @param gateTime 音長
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

    /** */
    public String toString() {
        return "Note:" +
            " duration=" + duration +
            " note=" + String.format("%02x", getNote())  +
            " gateTime=" + String.format("%04x", gateTime) +
            " velocity=" + String.format("%04x", velocity);
    }

    /* */
    @Override
    public byte[] getMessage() {
        return null; // TODO
    }

    /* */
    @Override
    public int getLength() {
        return 0;   // TODO
    }

private static int uc = 0;

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

if (gateTime == 0) {
 if (uc < 10) {
  Debug.println(Level.WARNING, "★★★★★ gateTime == 0 ignored: " + this);
 }
 uc++;
 return null;
}
        int length = (int) context.getTicksOf(this.gateTime);
        int pitch = context.retrievePitch(this.channel, getNote());
        int midiChannel = context.retrieveChannel(this.channel);
        int velocity = this.velocity == -1 ? context.getVelocity(this.channel) : context.setVelocity(this.channel, this.velocity);

        MidiEvent[] events = new MidiEvent[2];
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.NOTE_ON,
                                midiChannel,
                                pitch,
                                velocity);
//Debug.println("note: " + channel + ": " + pitch);
        events[0] = new MidiEvent(shortMessage, context.getCurrentTick());

        shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.NOTE_OFF,
                                midiChannel,
                                pitch,
                                0);
        events[1] = new MidiEvent(shortMessage, context.getCurrentTick() + length);

        return events;
    }

    /**
     * TODO Δタイムが、直前の同ボイス、同キーの NoteMessage のゲートタイムより小さい場合は直前の NoteMessage からの継続音とする
     * TODO 次の音まで余裕があったら伸ばして、無かったら切る？(未実装)
     */
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context)
        throws InvalidSmafDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int command = shortMessage.getCommand();
        int data1 = shortMessage.getData1();
        int data2 = shortMessage.getData2();
//Debug.println(midiEvent.getTick() + ", " + channel + ", " + command + ", " + (context.retrievePitch(channel, data1) + 45) + ", " + (data2 / 2));

        if (command == ShortMessage.NOTE_OFF ||
            // note on で velocity 0 の場合
            (command == ShortMessage.NOTE_ON && data2 == 0)) {

            if (!context.isNoteOffEventUsed()) {
Debug.println(Level.FINE, "[" + context.getMidiEventIndex() + "] no pair of ON for: " + channel + "ch, " + data1);
            }

            return null;
        } else /* if (command == ShortMessage.NOTE_ON) */ {

            MidiEvent noteOffEvent;

            try {
                noteOffEvent = context.getNoteOffMidiEvent();
            } catch (NoSuchElementException e) {
Debug.println(Level.WARNING, "[" + context.getMidiEventIndex() + "] no pair of OFF for: " + channel + "ch, " + data1);
                return null;
            }

            int track = context.retrieveSmafTrack(channel);
            int voice = context.retrieveVoice(channel);

            double scale = context.getScale();

            long currentTick = midiEvent.getTick();
            long noteOffTick = noteOffEvent.getTick();
            int length = (int) Math.round((noteOffTick - currentTick) / scale);

            int delta = context.getDuration();

            int onLength = (length + 254) / 255;
            SmafEvent[] smafEvents = new SmafEvent[1/* onLength */];
            for (int i = 0; i < onLength; i++) {

                NoteMessage smafMessage = new NoteMessage();
                smafMessage.setDuration(i == 0 ? delta : 0);
                smafMessage.setChannel(voice);
                smafMessage.setNote(context.retrievePitch(channel, data1));
                smafMessage.setGateTime(i == onLength - 1 ? length % 255 : 255);
if (length >= 255) {
 Debug.println(Level.FINE, channel + "ch, " + smafMessage.getNote() + ", " + smafMessage.getDuration() + ":[" + i + "]:" + (i == onLength - 1 ? length % 255 : 255) + "/" + length);
}
//Debug.println(channel + ", " + smafMessage.getVoice() + ", " + ((smafMessage.getMessage()[1] & 0xc0) >> 6));
                smafEvents[i] = new SmafEvent(smafMessage, 0L); // TODO 0l
if (smafEvents[i] == null) {
 Debug.println(Level.FINE, "[" + i + "]: " + smafEvents[i]);
}

                if (i == 0) {
                    context.setBeforeTick(track, midiEvent.getTick());
                    break;
                } else {
//                    context.incrementBeforeTick(track, i == onLength - 1 ? length % 255 : 255);
                }
            }

            return smafEvents;
        }
    }
}

/* */
