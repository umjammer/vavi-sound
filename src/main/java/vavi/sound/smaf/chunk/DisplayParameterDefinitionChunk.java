/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.InvalidSmafDataException;

import static java.lang.System.getLogger;


/**
 * DisplayParameterDefinition Chunk.
 * <pre>
 * "Gdpd"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class DisplayParameterDefinitionChunk extends Chunk {

    private static final Logger logger = getLogger(DisplayParameterDefinitionChunk.class.getName());

    /** */
    public DisplayParameterDefinitionChunk(byte[] id, int size) {
        super(id, size);
//logger.log(Level.TRACE, "DisplayParameterDefinition: " + size);
    }

    /** */
    public DisplayParameterDefinitionChunk() {
        System.arraycopy("Gdpd".getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        int i = 0;
        while (i < size) {
            int eventSize = MidiUtil.readVariableLength(dis);
            int eventType = dis.readUnsignedByte();
            Event event = new Event();
            event.eventType = eventType;
logger.log(Level.DEBUG, "event: " + eventType);
            for (int j = 0; j < ((eventSize - 1) / 2); j++) {
                int parameterId = dis.readUnsignedByte();
                int parameterValue = dis.readUnsignedByte();
                Event.Parameter parameter = new Event.Parameter();
                parameter.parameterID = ParameterID.valueOf(parameterId);
                parameter.value = parameterValue;
                event.parameters.add(parameter);
logger.log(Level.DEBUG, "parameters: " + parameter);
            }
            i += (eventSize > 127 ? 2 : 1) + eventSize;
        }
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        for (Event event : events) {
            event.writeTo(os);
        }
    }

    /** */
    private final List<Event> events = new ArrayList<>();

    /** */
    static class Event {
        /**
         * 0x00 Default Parameter
         * 0x40...0x7f
         */
        int eventType;
        /** */
        final List<Parameter> parameters = new ArrayList<>();
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
        /** font type */
        FontType(0x01),
        /** font size */
        FontSize(0x02),
        /** direction */
        Direction(0x03),
        /** attribute future expansion */
        Attribute(0x04),
        /** font color */
        FontColor0(0x10),
        /** color after changed */
        FontColor1(0x11),
        /** text border color future expansion */
        EdgeColor0(0x12),
        /** text border color after color change future expansion */
        EdgeColor1(0x13),
        /** text background color */
        BackColor0(0x14),
        /** text background color after color change */
        BackColor1(0x15),
        /** default coordinate specification method */
        Coordinates(0x20),
        /** background color plane 0 specified color */
        BackDropColor(0x30),
        /** specify the color to be transparent */
        TransparentColor(0x31),
        /** transparency processing enable flag */
        TransparentEnable(0x32);
        /** */
        final int value;
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
