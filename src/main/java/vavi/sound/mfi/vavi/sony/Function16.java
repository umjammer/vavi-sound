/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sony;

import java.lang.System.Logger.Level;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.MidiContext;
import vavi.sound.mfi.vavi.sequencer.MidiConvertibleFunction;
import vavi.sound.mfi.vavi.sequencer.YamahaMfiExclusive;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.YamahaAudioEngine;
import vavi.util.StringUtil;


/**
 * Sony System exclusive message function 0x10 processor.
 * (MA-3 / MA-5 extension)
 * <p>
 * The Sony MFi 3.0 writer ({@code _so40} files) puts the whole Yamaha MA-3 / MA-5
 * extension NEC writes as its level 0x01 messages into this one function, a sub
 * function byte selecting what the NEC group / function bytes do:
 * </p>
 * <pre>
 * 0     delta
 * 1     ff
 * 2     ff
 * 3-4   length
 * 5     vendor
 * 6     0x10
 * 7     76 543210
 *       ~~ ~~~~~~
 *       |  +- sub function
 *       +---- channel, the voice inside the track (only 0x09, 0x0a and 0x0c use it)
 * 8-    payload
 * </pre>
 * <table>
 * <tr><th>sub</th><th>payload</th><th>NEC level 0x01</th><th>meaning</th></tr>
 * <tr><td>0x01</td><td>1</td><td></td><td>? once at the head of track 0, 0 in 3149 of 3180</td></tr>
 * <tr><td>0x02</td><td>1</td><td></td><td>? once at the head of track 0, 1 in 2925 of 2928</td></tr>
 * <tr><td>0x04</td><td>2 + 31 (17)</td><td>f0.04</td><td>FM tone, bank, program, 4 (2) operator voice</td></tr>
 * <tr><td>0x05</td><td>2 + 16</td><td>f0.05</td><td>WT tone, bank, program, VM35 PCM voice</td></tr>
 * <tr><td>0x06</td><td>2 + n</td><td>f0.06</td><td>WT wave, wave id, format, wave</td></tr>
 * <tr><td>0x07</td><td>4 + n</td><td>f0.07</td><td>stream wave, stream, format, rate, adpcm</td></tr>
 * <tr><td>0x08</td><td>16</td><td>f2.07</td><td>channel status</td></tr>
 * <tr><td>0x09</td><td>2</td><td>f1.x3</td><td>StreamOn, stream, velocity</td></tr>
 * <tr><td>0x0a</td><td>1</td><td>f1.x5</td><td>StreamOff, stream</td></tr>
 * <tr><td>0x0c</td><td>1</td><td>f1.x7</td><td>Hold1, 0 ~ 127</td></tr>
 * <tr><td>0x11</td><td>1</td><td>f3.01 ?</td><td>FM mode setting, 0 or 1</td></tr>
 * </table>
 * <p>
 * How it was read, from the corpus at {@code ~/Public/np2/mfi} (3180 files write this
 * function, 23.8k messages), since no document was available:
 * </p>
 * <ul>
 *  <li>the payloads are NEC's. The {@code upload_melody} directories carry every song
 *      once per maker, and the 0x07 payload of a {@code _so40} file is the payload of
 *      the NEC {@code 01 f0 07} of the {@code _n40} file of the same song, header and
 *      adpcm alike, in 5 of the 6 songs compared (the 6th NEC one is shorter). An FM
 *      tone {@code 04 30 00 79 87 ...} and a 10000Hz drum WT tone
 *      {@code 84 00 27 10 79 ...} of the corpus start with the very bytes the NEC
 *      tone messages of {@code 威風堂々　クラシカル.mld} carry after their headers.</li>
 *  <li>so the tone records are NEC's {@code bank program <voice>} with no type byte (a
 *      message carries one record, so its length tells 2 from 4 operators), and like
 *      NEC's, the bank is always one the song selects with a
 *      {@link vavi.sound.mfi.vavi.track.ChangeBankMessage} and the program either one it
 *      selects with a {@link vavi.sound.mfi.vavi.track.ChangeVoiceMessage} (a melody
 *      voice, 536 of 536) or, bit 7 of the bank set, a note key it plays on that bank
 *      (a drum voice, 535 of 535).</li>
 *  <li>a WT tone that plays a wave of its own ({@code RM} 0) names a wave id a 0x06
 *      of the same file sends in all 119 cases, and every 0x06 wave is format 0, 4 bit
 *      adpcm.</li>
 *  <li>0x0c on a channel goes to 0x78 or 0x7f at the start of a phrase and back to 0 at
 *      its end, at the very places the {@code _n40} file of the same song writes the
 *      NEC Hold1 ({@code 01 f1 x7}) with the same value.</li>
 *  <li>0x09 / 0x0a come only in files with a 0x07, their first byte is a stream number
 *      those 0x07 send and 0x09's second one is 0x7f but for a few.</li>
 *  <li>0x11 is 1 exactly when the NEC {@code 01 f3 01} (FM mode setting) of the same song
 *      is 1 in 1343 of 1485 songs - more than chance, not a proof.</li>
 * </ul>
 * <p>
 * As with NEC's, the tones and the waves are handed to the synthesizer as the SMAF
 * exclusives they are ({@link YamahaMfiExclusive}), and the stream is played by a
 * {@link YamahaAudioEngine}, the adpcm being the one NEC's is. Hold1 is converted into
 * the MIDI hold pedal (control change 64) on the channel of its track and voice
 * ({@link MidiConvertibleFunction}); the other subs go to the synthesizer as the sysex.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function16 extends SonyFunction implements MidiConvertibleFunction {

    /** ? once at the head of track 0 */
    public static final int SUB_01 = 0x01;
    /** ? once at the head of track 0 */
    public static final int SUB_02 = 0x02;
    /** FM tone (voice) setting, bank, program, 17 or 31 byte voice */
    public static final int SUB_FM_TONE = 0x04;
    /** WT tone (voice) setting, bank, program, 16 byte voice */
    public static final int SUB_WT_TONE = 0x05;
    /** WT wave, wave id, format, wave */
    public static final int SUB_WT_WAVE = 0x06;
    /** stream wave, stream, format, sampling rate (2 bytes), adpcm */
    public static final int SUB_STREAM_WAVE = 0x07;
    /** channel status, 16 bytes */
    public static final int SUB_CHANNEL_STATUS = 0x08;
    /** StreamOn, stream, velocity */
    public static final int SUB_STREAM_ON = 0x09;
    /** StreamOff, stream */
    public static final int SUB_STREAM_OFF = 0x0a;
    /** Hold1, 0 ~ 127 */
    public static final int SUB_HOLD1 = 0x0c;
    /** FM mode setting, most likely */
    public static final int SUB_FM_MODE = 0x11;

    /** voice length of a 2 operator FM tone */
    public static final int FM2_VOICE = 17;
    /** voice length of a 4 operator FM tone */
    public static final int FM4_VOICE = 31;
    /** voice length of a WT tone */
    public static final int WT_VOICE = 16;
    /** the 0x06 format of 4 bit adpcm, the only one the corpus has */
    public static final int FORMAT_ADPCM = 0;

    /** MIDI note number of the drum voice whose program (note key) is 0 */
    private static final int DRUM_NOTE_OFFSET = 35;

    /** plays the stream waves, the same adpcm NEC's stream is */
    private static final AudioEngine engine = new YamahaAudioEngine();

    /** */
    static AudioEngine getAudioEngine() {
        return engine;
    }

    @Override
    int getFunction() {
        return 0x10;
    }

    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        if (data.length < 8) {
            throw new InvalidMfiDataException("sony 0x10 is too short: " + data.length);
        }
        this.channel     = (data[7] & 0xc0) >> 6;  // 0 ~ 3
        this.subFunction =  data[7] & 0x3f;
        this.data = Arrays.copyOfRange(data, 8, data.length);

        switch (subFunction) {
        case SUB_FM_TONE, SUB_WT_TONE -> {
            if (this.data.length < 2 + (subFunction == SUB_FM_TONE ? FM2_VOICE : WT_VOICE)) {
                throw new InvalidMfiDataException("sony 0x10 tone is too short: " + this.data.length);
            }
logger.log(Level.DEBUG, "%s tone: bank: %d%s, program: %d, voice: %d bytes"
        .formatted(subFunction == SUB_FM_TONE ? "FM" : "WT", getBank(), isDrum() ? " (drum)" : "", getProgram(), getVoice().length));
            YamahaMfiExclusive.send(receiver, YamahaMfiExclusive.voice(
                    0,
                    0,
                    MidiContext.toProgram(getBank(), getProgram()),
                    isDrum() ? getProgram() + DRUM_NOTE_OFFSET : 0,
                    subFunction == SUB_FM_TONE ? YamahaMfiExclusive.VoiceType.FM : YamahaMfiExclusive.VoiceType.PCM,
                    getVoice()));
        }
        case SUB_WT_WAVE -> {
logger.log(Level.DEBUG, "WT wave: No.%d, format: %d, %d bytes".formatted(getWaveNumber(), getFormat(), getWave().length));
            if (getFormat() == FORMAT_ADPCM) {
                YamahaMfiExclusive.send(receiver, YamahaMfiExclusive.wave(getWaveNumber(), getWave()));
            }
        }
        case SUB_STREAM_WAVE -> {
            int streamNumber = getStreamNumber();
            byte[] adpcm = getWave();
logger.log(Level.DEBUG, "stream wave: No.%d, %dHz, %d bytes, %s".formatted(streamNumber, getSamplingRate(), adpcm.length, isMono() ? "mono" : "stereo"));
logger.log(Level.TRACE, "data:\n" + StringUtil.getDump(data, 32));
            engine.setData(streamNumber, -1, getSamplingRate(), 4, isMono() ? 1 : 2, adpcm, false);
        }
        case SUB_STREAM_ON -> {
            int streamNumber = getStreamNumber(); // this instance is a shared singleton
logger.log(Level.DEBUG, "StreamOn: %dch, No.%d, velocity: %d".formatted(channel, streamNumber, this.data[1] & 0x7f));
            AudioEngine.Sync.schedule(() -> engine.start(streamNumber));
        }
        case SUB_STREAM_OFF -> {
            int streamNumber = getStreamNumber();
logger.log(Level.DEBUG, "StreamOff: %dch, No.%d".formatted(channel, streamNumber));
            AudioEngine.Sync.scheduleStop(() -> engine.stop(streamNumber));
        }
        case SUB_HOLD1 ->
logger.log(Level.DEBUG, "Hold1: %dch, %d".formatted(channel, getValue()));
        default ->
logger.log(Level.DEBUG, "sub 0x%02x: %dch\n%s".formatted(subFunction, channel, StringUtil.getDump(this.data, 32)));
        }
    }

    /** the MIDI hold pedal */
    private static final int CONTROL_CHANGE_HOLD1 = 64;

    @Override
    public MidiEvent[] getMidiEvents(byte[] data, MidiContext context)
        throws InvalidMidiDataException {

        if (data.length < 9 || (data[7] & 0x3f) != SUB_HOLD1) {
            return null;
        }
        int channel = ((data[7] & 0xc0) >> 6) + 4 * context.getMfiTrackNumber();
        ShortMessage shortMessage = new ShortMessage();
        shortMessage.setMessage(ShortMessage.CONTROL_CHANGE, context.retrieveChannel(channel), CONTROL_CHANGE_HOLD1, data[8] & 0x7f);
        return context.withOrigins(channel, new MidiEvent(shortMessage, context.getCurrent()));
    }

    /** channel 0 ~ 3 */
    private int channel;
    /** @see #SUB_FM_TONE SUB_* */
    private int subFunction;
    /** the payload after the sub function byte */
    private byte[] data = new byte[0];

    /** channel 0 ~ 3, the voice inside the track */
    public int getChannel() {
        return channel;
    }

    /** @see #SUB_FM_TONE SUB_* */
    public int getSubFunction() {
        return subFunction;
    }

    /** the payload after the sub function byte as it is */
    public byte[] getData() {
        return data;
    }

    /** the one byte value of {@link #SUB_HOLD1}, {@link #SUB_FM_MODE}, {@link #SUB_01} and {@link #SUB_02} */
    public int getValue() {
        return data[0] & 0x7f;
    }

    /** the bank of a tone, 0 ~ 63 */
    public int getBank() {
        return data[0] & 0x3f;
    }

    /** whether a tone is a drum voice, bit 7 of its bank */
    public boolean isDrum() {
        return (data[0] & 0x80) != 0;
    }

    /** the program of a melody tone, the note key of a drum tone */
    public int getProgram() {
        return data[1] & 0xff;
    }

    /** the voice of a tone, 17 or 31 bytes for {@link #SUB_FM_TONE}, 16 for {@link #SUB_WT_TONE} */
    public byte[] getVoice() {
        return Arrays.copyOfRange(data, 2, data.length);
    }

    /** the wave id of {@link #SUB_WT_WAVE} */
    public int getWaveNumber() {
        return data[0] & 0xff;
    }

    /** the format of {@link #SUB_WT_WAVE}, see {@link #FORMAT_ADPCM} */
    public int getFormat() {
        return data[1] & 0xff;
    }

    /** the wave of {@link #SUB_WT_WAVE}, the adpcm of {@link #SUB_STREAM_WAVE} */
    public byte[] getWave() {
        return Arrays.copyOfRange(data, subFunction == SUB_STREAM_WAVE ? 4 : 2, data.length);
    }

    /** the stream number of {@link #SUB_STREAM_WAVE}, {@link #SUB_STREAM_ON} and {@link #SUB_STREAM_OFF} */
    public int getStreamNumber() {
        return data[0] & 0x1f;
    }

    /** whether the {@link #SUB_STREAM_WAVE} is mono, bit 7 of its format byte as NEC's */
    public boolean isMono() {
        return (data[1] & 0x80) == 0;
    }

    /** the sampling rate of {@link #SUB_STREAM_WAVE} [Hz] */
    public int getSamplingRate() {
        return ((data[2] & 0xff) << 8) | (data[3] & 0xff);
    }

    /** channel 0 ~ 3 */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** @see #SUB_FM_TONE SUB_* */
    public void setSubFunction(int subFunction) {
        this.subFunction = subFunction & 0x3f;
    }

    /** the payload after the sub function byte */
    public void setData(byte[] data) {
        this.data = data;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[3 + data.length];
        tmp[0] = (byte) (VENDOR_SONY | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x10;
        tmp[2] = (byte) ((channel << 6) | subFunction);
        System.arraycopy(data, 0, tmp, 3, data.length);
        return tmp;
    }
}
