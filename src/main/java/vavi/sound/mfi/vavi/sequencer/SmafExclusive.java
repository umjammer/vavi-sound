/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.midi.MidiUtil.encode87;


/**
 * The SMAF (Yamaha) exclusives an MFi machine dependent tone or wave message
 * corresponds to.
 * <p>
 * An MFi tone message and a SMAF "VOIC" / "EXVO" / "EXWV" chunk describe the very
 * same MA-3 / MA-5 voice, only wrapped differently, so the way to hand an MFi
 * voice to a MIDI synthesizer is to hand it the SMAF exclusive it already speaks.
 * That is what {@link vavi.sound.smaf.message.yamaha.YamahaMessage} emits while a
 * SMAF file plays, and this class emits the same thing for MFi:
 * </p>
 * <pre>
 *  f0 45 7f &lt;encode87(43 ... f7)&gt; f7
 *     ~~ ~~
 *     |  +--- {@link #SYSEX_PACKED}, an 8 bit smaf exclusive packed into 7 bit bytes
 *     +------ {@link VaviMidiDeviceProvider#MANUFACTURER_ID}
 * </pre>
 * <p>
 * A synthesizer which wants the voices unpacks the payload and registers them,
 * one which does not simply ignores the manufacturer,
 * see {@code vavi.sound.midi.ymf262.NukedSynthesizer#processYamahaSmafSysexMessage}
 * of {@code ../vavi-apps-mfiplayer} for a receiving side.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see vavi.sound.smaf.message.yamaha.YamahaMessage
 */
public final class SmafExclusive {

    private static final Logger logger = getLogger(SmafExclusive.class.getName());

    private SmafExclusive() {
    }

    /** 7 bit packed sysex which carries an 8 bit smaf exclusive */
    public static final int SYSEX_PACKED = vavi.sound.smaf.message.MachineDependentMessage.SYSEX_PACKED;

    /** YAMAHA */
    private static final int MANUFACTURER = 0x43;

    /** end of exclusive */
    private static final int EOX = 0xf7;

    /**
     * The voice type, the ordinal is what the {@code vt} byte of a voice
     * exclusive carries.
     *
     * @see "https://github.com/but80/smaf825/blob/v1/smaf/enums/voice_type.go"
     */
    public enum VoiceType {
        /** fm */
        FM,
        /** wave table */
        PCM,
        /** with the AL (filter) section, no decoder known */
        AL
    }

    /**
     * The MA-5 (VM5) voice setting exclusive.
     * <pre>
     *  43 79 07 7f 01 mm ll pc dn vt &lt;voice&gt; f7
     *                 ~~ ~~ ~~ ~~ ~~
     *                 |  |  |  |  +--- {@link VoiceType}
     *                 |  |  |  +------ drum note, 0 for a melody voice
     *                 |  |  +--------- program
     *                 |  +------------ bank LSB
     *                 +--------------- bank MSB
     * </pre>
     *
     * @param voice the VM35 voice image, 17 (2 operator) or 31 (4 operator) bytes
     *              for {@link VoiceType#FM}, 16 bytes for {@link VoiceType#PCM}
     */
    public static byte[] voice(int bankMSB, int bankLSB, int program, int drumNote, VoiceType type, byte[] voice) {
        byte[] data = new byte[10 + voice.length + 1];

        data[0] = (byte) MANUFACTURER;
        data[1] = (byte) 0x79;
        data[2] = (byte) 0x07;      // MA-5 (VM5)
        data[3] = (byte) 0x7f;
        data[4] = (byte) 0x01;      // voice setting
        data[5] = (byte) (bankMSB & 0x7f);
        data[6] = (byte) (bankLSB & 0x7f);
        data[7] = (byte) (program & 0x7f);
        data[8] = (byte) (drumNote & 0x7f);
        data[9] = (byte) type.ordinal();

        System.arraycopy(voice, 0, data, 10, voice.length);
        data[data.length - 1] = (byte) EOX;

        return data;
    }

    /**
     * The wave (wave ram) exclusive, the body of a SMAF "EXWV" chunk.
     * <pre>
     *  43 05 00 &lt;wave id&gt; &lt;4 bit adpcm&gt; f7
     * </pre>
     *
     * @param waveId what the {@code RM, WaveID} byte of a wave table voice refers to
     * @see vavi.sound.smaf.chunk.EXWVChunk
     */
    public static byte[] wave(int waveId, byte[] wave) {
        byte[] data = new byte[4 + wave.length + 1];

        data[0] = (byte) MANUFACTURER;
        data[1] = (byte) 0x05;      // smaf phrase (MA-3 / MA-5 class)
        data[2] = (byte) 0x00;      // wave data
        data[3] = (byte) (waveId & 0xff);

        System.arraycopy(wave, 0, data, 4, wave.length);
        data[data.length - 1] = (byte) EOX;

        return data;
    }

    /**
     * Packs an 8 bit smaf exclusive the way
     * {@link vavi.sound.smaf.message.yamaha.YamahaMessage} does.
     *
     * @param exclusive 0: manufacturer id ... last: 0xf7, 8 bit
     */
    public static SysexMessage pack(byte[] exclusive) throws InvalidMidiDataException {
        byte[] encoded = new byte[exclusive.length * 8 / 7 + 1];
        int encodedLength = encode87(exclusive, encoded, 0, exclusive.length);

        byte[] data = new byte[2 + encodedLength + 1];
        data[0] = (byte) VaviMidiDeviceProvider.MANUFACTURER_ID;
        data[1] = (byte) SYSEX_PACKED;
        System.arraycopy(encoded, 0, data, 2, encodedLength);
        data[data.length - 1] = exclusive[exclusive.length - 1]; // 0xf7

        SysexMessage sysexMessage = new SysexMessage();
        sysexMessage.setMessage(0xf0, data, data.length);
        return sysexMessage;
    }

    /**
     * Packs and sends an 8 bit smaf exclusive.
     * <p>
     * A receiver which cannot take it is not an error here - the MFi message it
     * comes from is decoded either way - so this only logs when sending fails.
     * </p>
     *
     * @param receiver nullable
     * @param exclusive 0: manufacturer id ... last: 0xf7, 8 bit
     */
    public static void send(Receiver receiver, byte[] exclusive) {
        if (receiver == null) {
            return;
        }
        try {
            receiver.send(pack(exclusive), -1);
logger.log(Level.DEBUG, "smaf exclusive: " + exclusive.length + " bytes\n" + StringUtil.getDump(exclusive, 32));
        } catch (InvalidMidiDataException | RuntimeException e) {
logger.log(Level.WARNING, "cannot send a smaf exclusive: " + e);
        }
    }
}
