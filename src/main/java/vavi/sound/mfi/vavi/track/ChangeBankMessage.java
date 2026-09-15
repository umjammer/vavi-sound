/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import vavi.sound.mfi.ChannelMessage;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.MidiConvertible;
import vavi.sound.mfi.vavi.TrackChunk;
import vavi.sound.mfi.vavi.TrackMessage;
import vavi.sound.midi.VaviMidiDeviceProvider;


/**
 * ChangeBankMessage.
 * <pre>
 *  0xff, 0xe# Sound Source Control Information
 *  channel    true
 *  delta    ?
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.11 030821 nsano implements {@link MidiConvertible} <br>
 *          0.12 030920 nsano repackage <br>
 *          0.13 031203 nsano implements {@link ChannelMessage} <br>
 */
public class ChangeBankMessage extends vavi.sound.mfi.ShortMessage
    implements ChannelMessage, MidiConvertible, TrackMessage {

    /**
     * sysex function id: the mfi bank as it is, which the midi program keeps only bit 0 of
     * <pre>
     * 0xf0 0x45 0x04 channel bank 0xf7
     * </pre>
     * for a synthesizer of an mfi sound source (e.g. fuetrek, bank 0 and 0x34 are tone sets
     * of their own), the others do not know the function and let it go.
     */
    public static final int SYSEX_FUNCTION_ID_BANK = 0x04;

    /** */
    private int voice;
    /** GM the 6 bit */
    private int bank;

    @Override
    public boolean accept(String key) {
        return "255.b.225".equals(key);
    }

    /**
     * for {@link TrackChunk}
     * @param delta delta time
     * @param status
     * @param data1 0xe1
     * @param data2
     * <pre>
     *  76 543210
     *  ~~ ~~~~~~
     *  |  +- bank
     *  +- voice
     * </pre>
     */
    @Override
    public ChangeBankMessage init(int delta, int status, int data1, int data2) {
        super.init(delta, 0xff, 0xe1, data2);

        this.voice = (data2 & 0xc0) >> 6;
        this.bank  =  data2 & 0x3f;

        return this;
    }

    /** for {@link MidiConvertible} */
    public ChangeBankMessage init() {
        return (ChangeBankMessage) super.init(0, 0xff, 0xe1, 0);
    }

    /** */
    public int getBank() {
        return bank;
    }

    /** */
    public void setBank(int bank) {
        this.bank = bank & 0x3f;
        this.data[3] = (byte) ((this.data[3] & 0xc0) | this.bank);
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

    @Override
    public String toString() {
        return "ChangeBank:" +
               " voice=" + voice +
               " bank="  + bank;
    }

    // ----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context)
        throws InvalidMidiDataException {

        int channel = getVoice() + 4 * context.getMfiTrackNumber();
//logger.log(Level.TRACE, "track: "+context.getTrackNumber()+", voice: "+getVoice());

//logger.log(Level.TRACE, "bank[" + channel + "]: " + getBank());
        channel = context.setBank(channel, getBank());

        SysexMessage sysexMessage = new SysexMessage();
        byte[] data = {
                (byte) 0xf0,
                VaviMidiDeviceProvider.MANUFACTURER_ID,
                SYSEX_FUNCTION_ID_BANK,
                (byte) channel,
                (byte) getBank(),
                (byte) 0xf7
        };
        sysexMessage.setMessage(data, data.length);

        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.PROGRAM_CHANGE,
                channel,
                context.getProgram(channel),
                0);
        return new MidiEvent[] {
                new MidiEvent(sysexMessage, context.getCurrent()),
                new MidiEvent(shortMessage, context.getCurrent())
        };
    }
}
