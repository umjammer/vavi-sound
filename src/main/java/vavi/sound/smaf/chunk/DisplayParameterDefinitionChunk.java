/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;


/**
 * DisplayParameterDefinition Chunk.
 * <pre>
 * "Gdpd"
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class DisplayParameterDefinitionChunk extends Chunk {

    /** */
    public DisplayParameterDefinitionChunk(byte[] id, int size) {
        super(id, size);
//Debug.println("DisplayParameterDefinition: " + size);
    }

    /** */
    public DisplayParameterDefinitionChunk() {
        System.arraycopy("Gdpd".getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    /** */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {

        int i = 0;
        while (i < size) {
            int eventSize = readOneToTwo(is);
            int eventType = read(is);
            Event event = new Event();
            event.eventType = eventType;
Debug.println("event: " + eventType);
            for (int j = 0; j < ((eventSize - 1) / 2); j++) {
                int parameterId = read(is);
                int parameterValue = read(is);
                Event.Parameter parameter = new Event.Parameter();
                parameter.parameterID = ParameterID.valueOf(parameterId);
                parameter.value = parameterValue;
                event.parameters.add(parameter);
Debug.println("parameters: " + parameter);
            }
            i += (eventSize > 127 ? 2 : 1) + eventSize;
        }
    }

    /** */
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        for (Event event : events) {
            event.writeTo(os);
        }
    }

    /** */
    private List<Event> events = new ArrayList<Event>();

    /** */
    static class Event {
        /**
         * 0x00 Default Parameter
         * 0x40...0x7f
         */
        int eventType;
        /** */
        List<Parameter> parameters = new ArrayList<Parameter>();
        /** */
        static class Parameter {
            ParameterID parameterID;
            int value;
            public String toString() {
                return parameterID + ", " + value;
            }
        }
        /** */
        public void writeTo(OutputStream os) {
            // TODO
        }
    }

    /** */
    enum ParameterID {
        /** �t�H���g�̎�� */
        FontType(0x01),
        /** �t�H���g�̃T�C�Y */
        FontSize(0x02),
        /** �������ѕ��� */
        Direction(0x03),
        /** �����A�g���r���[�g�w�� �����g�� */
        Attribute(0x04),
        /** �����F */
        FontColor0(0x10),
        /** �F�ւ��㕶���F */
        FontColor1(0x11),
        /** ���������F �����g�� */
        EdgeColor0(0x12),
        /** �F�ւ��㕶�������F �����g�� */
        EdgeColor1(0x13),
        /** �����w�i�F */
        BackColor0(0x14),
        /** �F�ւ��㕶���w�i�F */
        BackColor1(0x15),
        /** �f�t�H���g�̍��W�w����@ */
        Coordinates(0x20),
        /** �w�i�F Plane 0 �w��F */
        BackDropColor(0x30),
        /** �����F�Ƃ���F���w�� */
        TransparentColor(0x31),
        /** ���������̗L���t���O */
        TransparentEnable(0x32);
        /** */
        int value;
        /** */
        ParameterID(int value) {
            this.value = value;
        }
        /** */
        static ParameterID valueOf(int value) {
            for (ParameterID parameterID : values()) {
                if (parameterID.value == value) {
                    return parameterID;
                }
            }
            throw new IllegalArgumentException(String.valueOf(value));
        }
    }
}

/* */
