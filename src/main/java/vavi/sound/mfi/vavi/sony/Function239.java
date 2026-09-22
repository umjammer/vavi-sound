/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sony;

import java.lang.System.Logger.Level;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;


/**
 * Sony System exclusive message function 0xef processor.
 * (Pitch Bend Range)
 * <pre>
 * 0     delta
 * 1     ff
 * 2     ff
 * 3-4   length (0x0003)
 * 5     vendor
 * 6     0xef
 * 7     7 65 43210
 *       ~ ~~ ~~~~~
 *       | |  +- range [semitone]
 *       | +---- voice
 *       +------ always 0
 * </pre>
 * <p>
 * Written once per voice at the head of a track of the Sony MFi 2.0 writer
 * ({@code _so16} files), which never writes the MFi
 * {@link vavi.sound.mfi.vavi.track.PitchBendRangeMessage} (0xe7), so it takes its place
 * for {@link PitchBendFunction}. Of the 12445 messages of the corpus at
 * {@code ~/Public/np2/mfi} the range is 2 in 11147 and 12 in 1274, the two usual
 * ranges, bit 7 is never set, and the voice field takes all of 0 ~ 3. Against the
 * {@code _n40} file of the same song of the {@code upload_melody} directories the
 * range equals the NEC one's 0xe7 of the same track and voice in all 433 voices
 * both write one for.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function239 extends SonyFunction {

    @Override
    int getFunction() {
        return 0xef;
    }

    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.voice = (data[7] & 0x60) >> 5;  // 0 ~ 3
        this.range =  data[7] & 0x1f;        // semitone
logger.log(Level.DEBUG, "Pitch Bend Range: %dch, %d".formatted(voice, range));
    }

    /** 0 ~ 3 */
    private int voice;
    /** semitone */
    private int range = 2;

    /** 0 ~ 3 */
    public int getVoice() {
        return voice;
    }

    /** semitone */
    public int getRange() {
        return range;
    }

    /** */
    public void setVoice(int voice) {
        this.voice = voice & 0x03;
    }

    /** */
    public void setRange(int range) {
        this.range = range & 0x1f;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[3];
        tmp[0] = (byte) (VENDOR_SONY | CARRIER_DOCOMO);
        tmp[1] = (byte) 0xef;
        tmp[2] = (byte) ((voice << 5) | range);
        return tmp;
    }
}
