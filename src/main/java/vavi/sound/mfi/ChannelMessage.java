/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;


/**
 * チャンネルナンバに依存する {@link MfiMessage} を表すクラスです。
 * <p>
 * MIDI にあわせるためにこの名前を Voice ... の代わりに用いています。
 * </p>
 * <li>javax.sound.midi パッケージにはない... いるのか？
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 031203 nsano initial version <br>
 */
public interface ChannelMessage {

    /** */
    int getVoice();

    /** */
    void setVoice(int voice);
}

/* */
