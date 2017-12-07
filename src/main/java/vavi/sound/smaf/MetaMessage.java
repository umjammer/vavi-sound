/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.util.Map;


/**
 * MetaMessage.
 * <pre>

[MIDI]

FF 7F nn mm
      ~~ ~~
      |  +- manufacturers id
      +---- length

[テンポ]
      FF 51 03 aa bb cc

[テキスト]
      FF 01 ll dd … dd

[著作権表示]
      FF 02 ll dd … dd

[キュー・ポイント]
      FF 07 05 53 54 41 52 54 (START)
      FF 07 04 53 54 4F 50 (STOP)

[XF キューポイント]
      FF 7F 04 43 7B 02 rr

[チャンネルステータス指定]
      FF 7F 14 43 02 00 04 dd ... dd

[MA-5 AL チャンネル指定]
      FF 7F 06 43 02 01 01 cc dd

[MA-5 V 発音チャンネル指定]
      FF 7F 06 43 02 01 02 cc dd



 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 * @see "ATS-MA5-SMAF_GL_133_HV.pdf"
 */
public class MetaMessage extends SmafMessage {

    /** TODO better way? */
    protected Map<String, Object> data;

    /** */
    public MetaMessage() {
    }

    /**
     * @param data TODO better way?
     */
    protected MetaMessage(Map<String, Object> data) {
        this.data = data;
    }

    /** */
    protected int type;

    /**
     * @param data TODO better way?
     * <p>
     * {@link javax.sound.midi.MetaMessage} nearly compatible.
     * </p>
     */
    public void setMessage(int type, Map<String, Object> data)
        throws InvalidSmafDataException {

        this.type = type;
        this.data = data;
    }

    /**
     * Meta 番号
     * <p>
     * {@link javax.sound.midi.MetaMessage} compatible.
     * </p>
     */
    public int getType() {
        return type & 0xff;
    }

    /**
     * data
     * <p>
     * {@link javax.sound.midi.MetaMessage} nearly compatible.
     * </p>
     * @return コピー
     */
    public Map<String, Object> getData() {
        return data;
    }

    /** */
    public String toString() {
        return "Meta: type=" + getType();
    }

    /* */
    @Override
    public byte[] getMessage() {
        return null; // TODO
    }

    /* */
    @Override
    public int getLength() {
        return 0;   // TODO
    }
}

/* */
