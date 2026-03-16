/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import vavi.sound.midi.MidiUtil;


/**
 * MetaMessage.
 * <pre>
 * [MIDI]
 *
 * [tempo]
 * FF 51 03 aa bb cc
 * 
 * [text]
 * FF 01 ll dd … dd
 * 
 * [copyright]
 * FF 02 ll dd … dd
 * 
 * [cue point]
 * FF 07 ll dd … dd
 *
 * FF 07 05 53 54 41 52 54 (START)
 * FF 07 04 53 54 4F 50 (STOP)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 * @see "ATS-MA5-SMAF_GL_133_HV.pdf"
 */
public class MetaMessage extends SmafMessage {

    /** */
    protected int type;

    /** */
    protected int length;

    /** TODO better way? */
    protected Map<String, Object> mapData;

    /** */
    protected byte[] data;

    /** */
    public MetaMessage() {
        duration = 0;
    }

    /**
     * @param mapData TODO better way?
     * <p>
     * {@link javax.sound.midi.MetaMessage} nearly compatible.
     * </p>
     */
    public void setMessage(int type, Map<String, Object> mapData)
            throws InvalidSmafDataException {

        this.type = type;
        this.mapData = mapData;
    }

    /**
     * data
     * <p>
     * {@link javax.sound.midi.MetaMessage} nearly compatible.
     * </p>
     * @return copied data
     */
    public Map<String, Object> getMapData() {
        return mapData;
    }

    /**
     * <p>
     * {@link javax.sound.midi.MetaMessage} compatible.
     * </p>
     */
    public void setMessage(int type, byte[] data, int length)
            throws InvalidSmafDataException {

        this.type = type;
        this.data = data;
        this.length = length;
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

    /** */
    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Meta: type=" + type;
    }

    @Override
    public byte[] getMessage() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(0xff);
            DataOutputStream dos = new DataOutputStream(baos);
            MidiUtil.writeVarInt(dos, length);
            dos.write(data, 0, length);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int getLength() {
        return length;
    }
}
