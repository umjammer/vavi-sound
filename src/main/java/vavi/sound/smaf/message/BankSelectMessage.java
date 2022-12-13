/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;
import vavi.util.Debug;


/**
 * BankSelectMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class BankSelectMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible, SmafConvertible {

    /** smaf channel */
    private int channel;

    /**
     * <pre>
     * HandyPhoneStandard: 0x00, 0x80: use standard bank map
     * MobileStandard:
     */
    private int bank;

    /** */
    public enum Significant {
        Least(0x20),
        Most(0x00);
        final int data1;
        Significant(int data1) {
            this.data1 = data1;
        }
    }

    /** */
    private Significant significant;

    /**
     * Creates BankSelectMessage (for HandyPhoneStandard).
     * @param duration
     * @param channel smaf channel
     * @param value bank 0x00 ~ 0xff
     */
    public BankSelectMessage(int duration, int channel, int value) {
        this.duration = duration;
        this.channel = channel;
        this.bank = value;
    }

    /**
     * Creates BankSelectMessage (for MobileStandard).
     * @param duration
     * @param channel smaf channel
     * @param value bank 0x00 ~ 0xff
     */
    public BankSelectMessage(int duration, int channel, int value, Significant significant) {
        this.duration = duration;
        this.channel = channel;
        this.bank = value;
        this.significant = significant;
    }

    /** for SmafConvertible */
    protected BankSelectMessage() {
    }

    /** */
    public int getBank() {
        return bank;
    }

    /** */
    public void setBank(int bank) {
        this.bank = bank & 0x7f;
    }

    /** */
    public int getChannel() {
        return channel;
    }

    /** */
    public void setChannel(int voice) {
        this.channel = voice & 0x03;
    }

    /** */
    public String toString() {
        return "BankSelect:" +
            " duration=" + duration +
            " channel=" + channel +
            " bank=" + bank;
    }

    //----

    /* */
    public byte[] getMessage() {
        return null; // TODO
    }

    /* */
    public int getLength() {
        return 0;   // TODO
    }

    /** */
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        if (significant == null) {              // HandyPhoneStandard
Debug.printf("BankSelect: [%d] %dch, 0x%02x", duration, channel, bank);
            int data2;
            if ((bank & 0x80) != 0) {
                context.setDrum(channel, MidiContext.ChannelConfiguration.PERCUSSION);
                data2 = 0x7d;
            } else {
                context.setDrum(channel, MidiContext.ChannelConfiguration.SOUND_SET);
                data2 = 0x7c;
            }

            if (context.getFormatType() == FormatType.HandyPhoneStandard) {
                return null;
            } else {
                int midiChannel = context.retrieveChannel(this.channel);

                MidiEvent[] events = new MidiEvent[2];

                ShortMessage shortMessage = new ShortMessage();
                shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                        midiChannel,
                                        0x00,       // バンクセレクト MSB
                                        data2);
                events[0] = new MidiEvent(shortMessage, context.getCurrentTick());

                shortMessage = new ShortMessage();
                shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                        midiChannel,
                                        0x20,       // バンクセレクト LSB
                                        bank & 0x7f);
                events[1] = new MidiEvent(shortMessage, context.getCurrentTick());

                return events;
            }
        } else {                                // MobileStandard
            int midiChannel = context.retrieveChannel(this.channel);

Debug.println("BankSelect(" + significant + "): [" + duration + "] " + midiChannel + "ch, " + bank);
            ShortMessage shortMessage = new ShortMessage();
            shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                    midiChannel,
                                    significant.data1,  // バンクセレクト MSB or LSB
                                    bank);
            return new MidiEvent[] {
                new MidiEvent(shortMessage, context.getCurrentTick())
            };
        }
    }

    /** TODO */
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context)
        throws InvalidSmafDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int data1 = shortMessage.getData1();

        int track = context.retrieveSmafTrack(channel);

        BankSelectMessage changeBankMessage = new BankSelectMessage();
        changeBankMessage.setDuration(context.getDuration());
        changeBankMessage.setChannel(channel % 4);
        changeBankMessage.setBank(data1);

        context.setBeforeTick(track, midiEvent.getTick());
//Debug.println(channel + ": " + StringUtil.toHex2(data1) + ", " + StringUtil.toHex2(changeVoiceMessage.getProgram()) + ", " + changeBankMessage.getBank());

        return new SmafEvent[] {
            new SmafEvent(changeBankMessage, midiEvent.getTick()),
        };
    }
}

/* */
