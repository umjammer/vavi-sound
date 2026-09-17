/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

import vavi.sound.midi.VaviMidiDeviceProvider;

import static vavi.sound.midi.MidiUtil.encode87;


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
 * the way every other smaf exclusive is ({@link #pack}):
 * </p>
 * <pre>
 *  f0 45 7f &lt;encode87(payload)&gt; f7
 *
 *  payload
 *   45 10 id ff cc bb sh sl &lt;data&gt; f7   {@link #wave}      a stream wave
 *   45 11 id vv ch f7                  {@link #on}        start a stream
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
    public static final int SYSEX_FUNCTION_ID_PACKED = 0x7f;

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

    /** the sample encoding of a stream wave, the ordinal is what the {@code ff} byte carries */
    public enum Format {
        /** 4 bit YAMAHA adpcm, {@code vavi.sound.adpcm.ma} */
        ADPCM,
        /** signed (2's complement) pcm, 16 bit ones big endian */
        SIGNED,
        /** offset binary pcm */
        UNSIGNED;

        /**
         * @param format the format number an {@link AudioEngine} is chosen by,
         *               {@code 1}: smaf adpcm, {@code 0x82}: MFi adpcm, {@code 0} / {@code 4}: signed pcm,
         *               {@code 5}: offset binary pcm, see {@code vavi.sound.smaf.vavi.chunk.WaveType}
         * @return null when unknown
         */
        public static Format valueOf(int format) {
            return switch (format) {
                case 1, 0x82 -> ADPCM;
                case 0, 4 -> SIGNED;
                case 5 -> UNSIGNED;
                default -> null;
            };
        }
    }

    /** whether a wave, a start, a stop ... goes as an exclusive instead of into an {@link AudioEngine} */
    public static boolean isEnabled() {
        return Boolean.getBoolean("vavi.sound.mobile.AudioEngine.disabled");
    }

    /**
     * @param id 1 ~ 127
     * @param data 8 bit, as it is in the file
     */
    public static byte[] wave(int id, Format format, int channels, int bits, int samplingRate, byte[] data) {
        byte[] exclusive = new byte[8 + data.length + 1];
        exclusive[0] = VaviMidiDeviceProvider.MANUFACTURER_ID;
        exclusive[1] = WAVE;
        exclusive[2] = (byte) (id & 0x7f);
        exclusive[3] = (byte) format.ordinal();
        exclusive[4] = (byte) channels;
        exclusive[5] = (byte) bits;
        exclusive[6] = (byte) ((samplingRate >> 8) & 0xff);
        exclusive[7] = (byte) (samplingRate & 0xff);
        System.arraycopy(data, 0, exclusive, 8, data.length);
        exclusive[exclusive.length - 1] = (byte) EOX;
        return exclusive;
    }

    /**
     * @param velocity 0 ~ 127
     * @param channel 0 ~ 3, {@link #NO_CHANNEL} when the stream has none
     */
    public static byte[] on(int id, int velocity, int channel) {
        return new byte[] {VaviMidiDeviceProvider.MANUFACTURER_ID, ON, (byte) (id & 0x7f), (byte) (velocity & 0x7f), (byte) (channel & 0x7f), (byte) EOX};
    }

    /** */
    public static byte[] off(int id) {
        return new byte[] {VaviMidiDeviceProvider.MANUFACTURER_ID, OFF, (byte) (id & 0x7f), (byte) EOX};
    }

    /** @param volume 0 ~ 127 */
    public static byte[] volume(int channel, int volume) {
        return new byte[] {VaviMidiDeviceProvider.MANUFACTURER_ID, VOLUME, (byte) (channel & 0x7f), (byte) (volume & 0x7f), (byte) EOX};
    }

    /** @param panpot 0 ~ 127, center 64 */
    public static byte[] panpot(int channel, int panpot) {
        return new byte[] {VaviMidiDeviceProvider.MANUFACTURER_ID, PANPOT, (byte) (channel & 0x7f), (byte) (panpot & 0x7f), (byte) EOX};
    }

    /**
     * Packs an 8 bit smaf exclusive the way
     * {@link vavi.sound.smaf.vavi.message.yamaha.YamahaMessage} does.
     *
     * @param exclusive 0: manufacturer id ... last: 0xf7, 8 bit
     */
    public static SysexMessage pack(byte[] exclusive) throws InvalidMidiDataException {
        byte[] encoded = new byte[exclusive.length * 8 / 7 + 1];
        int encodedLength = encode87(exclusive, encoded, 0, exclusive.length);

        // pack 7bit
        byte[] data = new byte[2 + encodedLength + 1];
        data[0] = (byte) VaviMidiDeviceProvider.MANUFACTURER_ID;
        data[1] = (byte) SYSEX_FUNCTION_ID_PACKED;
        System.arraycopy(encoded, 0, data, 2, encodedLength);
        data[data.length - 1] = exclusive[exclusive.length - 1]; // 0xf7

        SysexMessage sysexMessage = new SysexMessage();
        sysexMessage.setMessage(0xf0, data, data.length);
        return sysexMessage;
    }
}
