/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import javax.sound.midi.MidiEvent;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;



/**
 * SmafConvertible.
 * <p>
 * 今のところ実装クラスは bean でなければならない．
 * (引数なしのコンストラクタがあること)
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano port from MFi <br>
 */
public interface SmafConvertible {

    /** TODO 実装法いまいち，BeanUtil 等が使えないか？ */
    SmafEvent[] getSmafEvents(MidiEvent midiEvent, SmafContext context) throws InvalidSmafDataException;
}

/* */
