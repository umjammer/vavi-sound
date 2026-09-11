/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import vavi.sound.midi.VaviMidiDeviceProvider;

import static vavi.sound.midi.MidiUtil.decode87;


/**
 * A {@link Receiver} which collects the SMAF exclusives {@link SmafExclusive}
 * sends, unpacked back to 8 bit.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
public class SmafExclusiveCapture implements Receiver {

    /** the 8 bit exclusives, "43 ... f7" each */
    private final List<byte[]> exclusives = new ArrayList<>();

    /** the 8 bit exclusives, "43 ... f7" each */
    public List<byte[]> getExclusives() {
        return exclusives;
    }

    /** */
    public void clear() {
        exclusives.clear();
    }

    /** the only exclusive collected so far */
    public byte[] getOnly() {
        if (exclusives.size() != 1) {
            throw new IllegalStateException("not exactly one exclusive: " + exclusives.size());
        }
        return exclusives.get(0);
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (!(message instanceof SysexMessage sysexMessage)) {
            return;
        }
        byte[] data = sysexMessage.getData();
        if (data.length < 3 ||
                (data[0] & 0xff) != (VaviMidiDeviceProvider.MANUFACTURER_ID & 0xff) ||
                (data[1] & 0xff) != SmafExclusive.SYSEX_PACKED) {
            return;
        }
        byte[] encoded = Arrays.copyOfRange(data, 2, data.length - 1);
        byte[] decoded = new byte[encoded.length];
        int n = decode87(encoded, decoded, 0, encoded.length);
        exclusives.add(Arrays.copyOf(decoded, n));
    }

    @Override
    public void close() {
    }
}
