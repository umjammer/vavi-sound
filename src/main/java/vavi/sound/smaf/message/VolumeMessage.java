/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;
import vavi.sound.smaf.message.MidiContext.ChannelConfiguration;
import vavi.util.Debug;


/**
 * VolumeMessage.
 * <pre>
 *  duration    1or2
 *  data0       0x00
 *  data1       cc 11 0111
 *  data2       value       0x00〜0x7f
 * </pre>
 * 
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public class VolumeMessage extends vavi.sound.smaf.ShortMessage
    implements MidiConvertible, SmafConvertible {

    /** smaf channel */
    private int channel;

    /** 0 - 127 */
    private int volume;

    /**
     *
     * @param duration
     * @param channel
     * @param value volume
     */
    public VolumeMessage(int duration, int channel, int value) {
        this.duration = duration;
        this.channel = channel & 0x03;
        this.volume =  value & 0x7f;
    }

    /** for SmafConvertible */
    public VolumeMessage() {
    }

    /** */
    public int getVolume() {
        return volume;
    }

    /** */
    public void setVolume(int volume) {
        this.volume = volume & 0x7f;
    }

    /** */
    public int getChannel() {
        return channel;
    }

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    public String toString() {
        return "Volume:" +
            " duration=" + duration +
            " channel=" + channel +
            " volume=" + volume;
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
            baos.write((channel << 6) | 0x37);
            baos.write(volume);
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

    /** */
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        if (context.getFormatType() == FormatType.HandyPhoneStandard &&
            context.getDrum(this.channel) == ChannelConfiguration.PERCUSSION &&
            context.getSmafTrackNumber() * 4 + this.channel != MidiContext.CHANNEL_DRUM) {

            // TODO psm は最後(最大？)？の volume を設定
Debug.println("volume: " + volume);
            context.setVelocity(this.channel, volume);

            return null;
        } else {
            int midiChannel = context.retrieveChannel(this.channel);

            MidiEvent[] events = new MidiEvent[1];
            ShortMessage shortMessage = new ShortMessage();
            shortMessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                    midiChannel,
                                    7,    // メインボリューム MSB
                                    volume);
            events[0] = new MidiEvent(shortMessage, context.getCurrentTick());
            return events;
        }
    }

    /** */
    public SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context)
        throws InvalidSmafDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int data2 = shortMessage.getData2();

        int track = context.retrieveSmafTrack(channel);
        int voice = context.retrieveVoice(channel);

        VolumeMessage smafMessage = new VolumeMessage();
        smafMessage.setDuration(context.getDuration());
        smafMessage.setChannel(voice);
        smafMessage.setVolume(data2);
Debug.println("voice: " + voice + ", volume: " + data2);

        context.setBeforeTick(track, midiEvent.getTick());

        return new SmafEvent[] {
            new SmafEvent(smafMessage, midiEvent.getTick())
        };
    }
}

/* */
