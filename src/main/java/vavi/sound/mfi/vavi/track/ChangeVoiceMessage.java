/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.mfi.ChannelMessage;
import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.vavi.MfiContext;
import vavi.sound.mfi.vavi.MfiConvertible;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;


/**
 * ChangeVoiceMessage.
 * <pre>
 *  0xff, 0xe# 音源制御情報
 *  channel true
 *  delta   ?
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.11 030821 nsano implements MidiConvertible <br>
 *          0.12 030906 nsano implements MfiConvertible <br>
 *          0.13 030920 nsano repackage <br>
 *          0.14 031203 nsano implements ChannelMessage <br>
 */
public class ChangeVoiceMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, MfiConvertible {

    /** */
    private int voice;
    /** GM lower 6 bits */
    private int program;

    /**
     * for {@link vavi.sound.mfi.vavi.TrackMessage}
     * @param delta delta time
     * @param status
     * @param data1 0xe0
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- program
     *  +- voice
     * </pre>
     */
    public ChangeVoiceMessage(int delta, int status, int data1, int data2) {
        super(delta, 0xff, 0xe0, data2);

        this.voice   = (data2 & 0xc0) >> 6;
        this.program =  data2 & 0x3f;
    }

    /** for {@link MfiConvertible} */
    public ChangeVoiceMessage() {
        super(0, 0xff, 0xe0, 0);
    }

    /** */
    public int getProgram() {
        return program;
    }

    /** */
    public void setProgram(int program) {
        this.program = program & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0xc0) | this.program);
    }

    @Override
    public int getVoice() {
        return voice;
    }

    @Override
    public void setVoice(int voice) {
        this.voice = voice & 0x03;
        this.data[3] = (byte) ((this.data[3] & 0x3f) | (this.voice << 6));
    }

    /** */
    public String toString() {
        return "ChangeVoice:" +
            " voice="   + voice +
            " program=" + program;
    }

    //----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int channel = getVoice() + 4 * context.getMfiTrackNumber();

//Debug.println("program[" + channel + "]: " + StringUtil.toHex2(getProgram()));
        channel = context.setProgram(channel, getProgram());

        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.PROGRAM_CHANGE,
                                channel,
                                context.getProgram(channel),
                                0);
        return new MidiEvent[] {
            new MidiEvent(shortMessage, context.getCurrent())
        };
    }

    @Override
    public MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException {

        ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
        int channel = shortMessage.getChannel();
        int data1 = shortMessage.getData1();

        int track = context.retrieveMfiTrack(channel);
        int voice = context.retrieveVoice(channel);

        ChangeVoiceMessage changeVoiceMessage = new ChangeVoiceMessage();
        changeVoiceMessage.setVoice(voice);
        changeVoiceMessage.setProgram(channel == 9 ? 0 : data1);

        ChangeBankMessage changeBankMessage = new ChangeBankMessage();
        changeBankMessage.setDelta(context.getDelta(context.retrieveMfiTrack(channel)));
        changeBankMessage.setVoice(channel % 4);
        changeBankMessage.setBank(((data1 & 0xc0) >> 6) + 2); // TODO 2 ???

        context.setPreviousTick(track, midiEvent.getTick());
//Debug.println(channel + ": " + StringUtil.toHex2(data1) + ", " + StringUtil.toHex2(changeVoiceMessage.getProgram()) + ", " + changeBankMessage.getBank());

        return new MfiEvent[] {
            new MfiEvent(changeBankMessage, midiEvent.getTick()),
            new MfiEvent(changeVoiceMessage, midiEvent.getTick())
        };
    }
}
