/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

import vavi.sound.midi.VaviMidiDeviceProvider;


/**
 * The mfi values midi has no room for, as they are, for a synthesizer of an mfi
 * sound source (e.g. fuetrek). The midi messages converted as before go along
 * with them, so a synthesizer which does not know this function lets them go
 * and nothing changes for it.
 * <pre>
 * 0xf0 0x45 0x04 sub ... 0xf7
 *
 * sub 0x01 bank          channel bank                mfi 0xe1, the midi program keeps only bit 0 of the bank
 * sub 0x02 master volume volume                      mfi 0xb0, the universal master volume following is this one
 * sub 0x03 pitch bend    channel fine                mfi 0xe9, the low 6 bits of the pitch bend 0xe4 following
 * sub 0x04 pitch bend range channel value            mfi 0xe7, the rpn 0 following is this one
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-15 nsano initial version <br>
 */
public final class FuetrekMfiExclusive {

    private FuetrekMfiExclusive() {
    }

    /** vavi sysex function id */
    public static final int SYSEX_FUNCTION_ID = 0x04;

    /** sub id: mfi bank */
    public static final int BANK = 0x01;
    /** sub id: mfi master volume */
    public static final int MASTER_VOLUME = 0x02;
    /** sub id: mfi fine pitch bend */
    public static final int PITCH_BEND_FINE = 0x03;
    /** sub id: mfi pitch bend range */
    public static final int PITCH_BEND_RANGE = 0x04;

    /** @return f0 45 04 sub data... f7 */
    public static SysexMessage message(int sub, int... data) throws InvalidMidiDataException {
        byte[] bytes = new byte[data.length + 5];
        bytes[0] = (byte) 0xf0;
        bytes[1] = VaviMidiDeviceProvider.MANUFACTURER_ID;
        bytes[2] = SYSEX_FUNCTION_ID;
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
                && message[1] == VaviMidiDeviceProvider.MANUFACTURER_ID && message[2] == SYSEX_FUNCTION_ID) {
            return message[3] & 0x7f;
        }
        return -1;
    }
}
