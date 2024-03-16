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

[tempo]
      FF 51 03 aa bb cc

[text]
      FF 01 ll dd … dd

[copyright]
      FF 02 ll dd … dd

[cue point]
      FF 07 05 53 54 41 52 54 (START)
      FF 07 04 53 54 4F 50 (STOP)

[XF cue point]
      FF 7F 04 43 7B 02 rr

[specify channel status]
      FF 7F 14 43 02 00 04 dd ... dd

[MA-5 AL specify channel ]
      FF 7F 06 43 02 01 01 cc dd

[MA-5 V specify voice channel]
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
     * Meta number
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
     * @return copied data
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
