/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import javax.sound.midi.MidiEvent;
import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;


/**
 * MfiConvertible
 * <p>
 * 今のところ実装クラスは bean でなければならない．
 * (引数なしのコンストラクタがあること)
 * </p>
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030905 nsano initial version <br>
 */
public interface MfiConvertible {

    /** TODO 実装法いまいち，BeanUtil 等が使えないか？ */
    MfiEvent[] getMfiEvents(MidiEvent midiEvent, MfiContext context)
        throws InvalidMfiDataException;
}

/* */
