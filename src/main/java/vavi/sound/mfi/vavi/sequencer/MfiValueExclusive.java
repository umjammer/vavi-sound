/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

import static vavi.sound.mfi.vavi.VaviMfiDeviceProvider.MANUFACTURER_ID;


/**
 * The mfi values midi has no room for, as they are, for a synthesizer of an mfi
 * sound source, whichever vendor's. The midi messages converted as before go
 * along with them, so a synthesizer which does not know this function lets them
 * go and nothing changes for it.
 * <pre>
 * 0xf0 0x45 0x04 sub ... 0xf7
 *
 * sub 0x01 bank          channel bank                mfi 0xe1, the midi program keeps only bit 0 of the bank
 * sub 0x02 master volume volume                      mfi 0xb0, the universal master volume following is this one
 * sub 0x03 pitch bend    channel fine                mfi 0xe9, the low 6 bits of the pitch bend 0xe4 following
 * sub 0x04 pitch bend range channel value            mfi 0xe7, the rpn 0 following is this one
 * sub 0x05 channel configuration channel raw raw7   mfi 0xba, channel is the midi one the mfi channel went to,
 *                                                    raw the low 7 bits of the mfi byte, raw7 its bit 7
 * sub 0x06 expression    channel value               mfi 0xe6, the 6 bit value as it is, the midi expression can't keep it
 * sub 0x07 channel       channel mfi channel         the next note on / off, program change or bank exclusive on the
 *                                                    channel is the mfi channel's, sent when the channel is not its own
 *                                                    (a percussion one gathered to 9, a melody one on 9 moved away)
 * sub 0x08 program       channel program             mfi 0xe0, the program as it is, sent when the midi one following
 *                                                    is not it (a percussion one is made 0)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-15 nsano initial version <br>
 *          0.01 2026-09-18 nsano rename from FuetrekMfiExclusive, not a vendor's <br>
 */
public final class MfiValueExclusive {

    private MfiValueExclusive() {
    }

    /** vavi sysex function id */
    public static final int MFi_SYSEX_FUNCTION_ID_VALUE = 0x04;

    /** sub id: mfi bank */
    public static final int BANK = 0x01;
    /** sub id: mfi master volume */
    public static final int MASTER_VOLUME = 0x02;
    /** sub id: mfi fine pitch bend */
    public static final int PITCH_BEND_FINE = 0x03;
    /** sub id: mfi pitch bend range */
    public static final int PITCH_BEND_RANGE = 0x04;
    /** sub id: mfi channel configuration (drum enable) */
    public static final int CHANNEL_CONFIGURATION = 0x05;
    /** sub id: mfi expression */
    public static final int EXPRESSION = 0x06;
    /** sub id: the mfi channel of the next channel message */
    public static final int CHANNEL = 0x07;
    /** sub id: mfi program */
    public static final int PROGRAM = 0x08;

    /** @return f0 45 04 sub data... f7 */
    public static SysexMessage message(int sub, int... data) throws InvalidMidiDataException {
        byte[] bytes = new byte[data.length + 5];
        bytes[0] = (byte) 0xf0;
        bytes[1] = MANUFACTURER_ID;
        bytes[2] = MFi_SYSEX_FUNCTION_ID_VALUE;
        bytes[3] = (byte) sub;
        for (int i = 0; i < data.length; i++) {
            bytes[4 + i] = (byte) (data[i] & 0x7f);
        }
        bytes[bytes.length - 1] = (byte) 0xf7;
        SysexMessage message = new SysexMessage();
        message.setMessage(bytes, bytes.length);
        return message;
    }

    /**
     * @param message a whole sysex, f0 included
     * @return the sub id, -1 if it is not this function
     */
    public static int sub(byte[] message) {
        if (message.length >= 5 && (message[0] & 0xff) == 0xf0
                && message[1] == MANUFACTURER_ID && message[2] == MFi_SYSEX_FUNCTION_ID_VALUE) {
            return message[3] & 0x7f;
        }
        return -1;
    }
}
