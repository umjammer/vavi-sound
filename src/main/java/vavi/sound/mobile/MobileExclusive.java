/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.text.Format;
import java.util.Arrays;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

import static vavi.sound.midi.MidiUtil.decode87;
import static vavi.sound.midi.MidiUtil.encode87;
import static vavi.sound.midi.VaviMidiDeviceProvider.MANUFACTURER_ID;


/**
 * The exclusives a stream PCM (ADPCM) wave travels as when
 * {@code vavi.sound.mobile.AudioEngine.disabled} is set, that is when the
 * MIDI synthesizer, not an {@link AudioEngine} in front of it, plays the wave.
 * <p>
 * An MA-3 / MA-5 chip has no MIDI exclusive for a stream wave: the driver
 * ({@code MaSndDrv_SetStream}, {@code MASNDDRV_CMD_STREAM_ON / OFF}) hands the
 * "Mwa*" chunk to the chip directly, and a note on a stream channel starts it.
 * So the wave data and the start / stop of an MFi audio message or a SMAF PCM
 * audio track need one of their own, which is this, sent packed 8 bit into 7
 * the way every other smaf exclusive is ({@link #packedSysex}):
 * </p>
 * <pre>
 *  f0 45 7f &lt;encode87(payload)&gt; f7
 *
 *  payload
 *   45 10 id ff cc bb sh sl &lt;data&gt; f7   {@link #wave}      a stream wave
 *   45 11 id vv ch [g2 g1 g0] f7       {@link #on}        start a stream, g: optional gate time [ms], big endian
 *   45 12 id f7                        {@link #off}       stop a stream
 *   45 13 ch vv f7                     {@link #volume}    volume of an MFi audio channel
 *   45 14 ch pp f7                     {@link #panpot}    panpot of an MFi audio channel
 *      ~~ ~~ ~~ ~~ ~~ ~~~~~
 *      |  |  |  |  |  +--- sampling rate [Hz], big endian
 *      |  |  |  |  +------ bits per sample
 *      |  |  |  +--------- channels, a stereo adpcm wave is L then R, a pcm one interleaved
 *      |  |  +------------ {@link Format}, vv: velocity / volume 0 ~ 127, ch: 0 ~ 3, 0x7f none
 *      |  +--------------- stream id, 1 ~ (the number of the "Mwa*" / "Awa*" chunk, the MFi audio data)
 *      +------------------ sub id
 * </pre>
 * <p>
 * Where the chip does have an exclusive this is not used: the stream panpot of
 * a SMAF file is {@code 43 79 0x 7f 0b id pp dd}, the wave of a wave table
 * voice {@code 43 79 0x 7f 03} or {@code 43 05 00}, and those go as they are.
 * </p>
 * <p>
 * A SMAF "Mobile Standard" stream note needs none of this either: it is a note
 * on a channel whose bank select MSB is {@code 0x7d} with a key of 0 ~ 12
 * (wave id = key + 1) or 92 ~ 110 (wave id = key - 78), see
 * {@code Note_ON3} of the MA-3 driver ({@code mammfcnv.c}).
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-13 nsano initial version <br>
 */
public final class MobileExclusive {

    private MobileExclusive() {
    }

    /** 7bit packed sysex message for 8bit smaf sysex message */
    public static final int MIDI_SYSEX_FUNCTION_ID_PACKED = 0x7f;

    /** sub id: a stream wave */
    public static final int WAVE = 0x10;
    /** sub id: start a stream */
    public static final int ON = 0x11;
    /** sub id: stop a stream */
    public static final int OFF = 0x12;
    /** sub id: volume of an audio channel */
    public static final int VOLUME = 0x13;
    /** sub id: panpot of an audio channel */
    public static final int PANPOT = 0x14;

    /** no channel */
    public static final int NO_CHANNEL = 0x7f;

    /** end of exclusive */
    private static final int EOX = 0xf7;

    /**
     * @param functionId mfi/smaf sysex function id
     * @param id 1 ~ 127
     * @param data 8 bit, as it is in the file
     */
    public static byte[] wave(int functionId, int id, int format, int channels, int bits, int samplingRate, byte[] data) {
        byte[] exclusive = new byte[9 + data.length + 1];
        exclusive[0] = (byte) MANUFACTURER_ID;
        exclusive[1] = (byte) functionId;
        exclusive[2] = WAVE;
        exclusive[3] = (byte) (id & 0x7f);
        exclusive[4] = (byte) format;
        exclusive[5] = (byte) channels;
        exclusive[6] = (byte) bits;
        exclusive[7] = (byte) ((samplingRate >> 8) & 0xff);
        exclusive[8] = (byte) (samplingRate & 0xff);
        System.arraycopy(data, 0, exclusive, 9, data.length);
        exclusive[exclusive.length - 1] = (byte) EOX;
        return exclusive;
    }

    /**
     * @param functionId mfi/smaf sysex function id
     * @param velocity 0 ~ 127
     * @param channel 0 ~ 3, {@link #NO_CHANNEL} when the stream has none
     */
    public static byte[] on(int functionId, int id, int velocity, int channel) {
        return new byte[] {(byte) MANUFACTURER_ID, (byte) functionId, ON, (byte) (id & 0x7f), (byte) (velocity & 0x7f), (byte) (channel & 0x7f), (byte) EOX};
    }

    /**
     * A start that also tells how long the stream sounds, a SMAF wave event
     * has its gate time with it, an audio engine plays exactly that long.
     *
     * @param functionId mfi/smaf sysex function id
     * @param velocity 0 ~ 127
     * @param channel 0 ~ 3, {@link #NO_CHANNEL} when the stream has none
     * @param gateTime [ms] 0 ~ 0xffffff
     */
    public static byte[] on(int functionId, int id, int velocity, int channel, long gateTime) {
        return new byte[] {(byte) MANUFACTURER_ID, (byte) functionId, ON, (byte) (id & 0x7f), (byte) (velocity & 0x7f), (byte) (channel & 0x7f),
                (byte) ((gateTime >> 16) & 0xff), (byte) ((gateTime >> 8) & 0xff), (byte) (gateTime & 0xff), (byte) EOX};
    }

    /**
     * @param on the start exclusive from the sub id: 11 id vv ch [g2 g1 g0] (f7)
     * @return gate time [ms], -1 when the start has none
     */
    public static long gateTime(byte[] on) {
        if (on.length < 7) {
            return -1;
        }
        return ((on[4] & 0xffL) << 16) | ((on[5] & 0xffL) << 8) | (on[6] & 0xffL);
    }

    /**
     * @param functionId mfi/smaf sysex function id
     */
    public static byte[] off(int functionId, int id) {
        return new byte[] {(byte) MANUFACTURER_ID, (byte) functionId, OFF, (byte) (id & 0x7f), (byte) EOX};
    }

    /**
     * @param functionId mfi/smaf sysex function id
     * @param volume 0 ~ 127
     */
    public static byte[] volume(int functionId, int channel, int volume) {
        return new byte[] {(byte) MANUFACTURER_ID, (byte) functionId, VOLUME, (byte) (channel & 0x7f), (byte) (volume & 0x7f), (byte) EOX};
    }

    /**
     * @param functionId mfi/smaf sysex function id
     * @param panpot 0 ~ 127, center 64
     */
    public static byte[] panpot(int functionId, int channel, int panpot) {
        return new byte[] {(byte) MANUFACTURER_ID, (byte) functionId, PANPOT, (byte) (channel & 0x7f), (byte) (panpot & 0x7f), (byte) EOX};
    }

    /**
     * Packs an 8 bit smaf exclusive the way
     * {@link vavi.sound.mfi.vavi.track.MachineDependentMessage},
     * {@link vavi.sound.smaf.vavi.message.yamaha.YamahaMessage} does.
     */
    public static SysexMessage packedSysex(byte[] exclusive) throws InvalidMidiDataException {
        byte[] data = pack(exclusive);

        SysexMessage sysexMessage = new SysexMessage();
        sysexMessage.setMessage(0xf0, data, data.length);
        return sysexMessage;
    }

    /**
     * @param exclusive 0: manufacturer id ... last: 0xf7, 8 bit
     */
    public static byte[] pack(byte[] exclusive) {
        byte[] encoded = new byte[exclusive.length * 8 / 7 + 1];
        int encodedLength = encode87(exclusive, encoded, 0, exclusive.length);

        // pack 7bit
        byte[] data = new byte[2 + encodedLength + 1];
        data[0] = (byte) MANUFACTURER_ID;
        data[1] = (byte) MIDI_SYSEX_FUNCTION_ID_PACKED;
        System.arraycopy(encoded, 0, data, 2, encodedLength);
        data[data.length - 1] = exclusive[exclusive.length - 1]; // 0xf7

        return data;
    }

    /**
     * @param data 45 7f packed 7bit data ... 7f
     */
    public static byte[] unpack(byte[] data) {
        // (f0) 45 7f {encoded ...} f7, the packer encodes the whole exclusive
        // including its own trailing 0xf7 and then repeats that 0xf7 raw, so every
        // encoded byte is data[2] ... data[length - 2] and the decoded exclusive
        // already ends with 0xf7. Cutting one byte short here loses the last
        // block's high bit flags, which shows up as stray 0x80s in the tail of a
        // voice.
        assert data[0] == MANUFACTURER_ID && data[1] == MIDI_SYSEX_FUNCTION_ID_PACKED : "not vavi packed";
        byte[] encoded = Arrays.copyOfRange(data, 2, data.length - 1);
        byte[] decoded = new byte[((encoded.length + 1) * 7) / 8]; // for 8bits data
        int n = decode87(encoded, decoded, 0, encoded.length);
        return Arrays.copyOf(decoded, n);
    }
}
