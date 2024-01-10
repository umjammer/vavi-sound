/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;


/**
 * DisplayParameterDefinition Chunk.
 * <pre>
 * "Gdpd"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
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

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        int i = 0;
        while (i < size) {
            int eventSize = MidiUtil.readVariableLength(dis);
            int eventType = dis.readUnsignedByte();
            Event event = new Event();
            event.eventType = eventType;
Debug.println(Level.FINE, "event: " + eventType);
            for (int j = 0; j < ((eventSize - 1) / 2); j++) {
                int parameterId = dis.readUnsignedByte();
                int parameterValue = dis.readUnsignedByte();
                Event.Parameter parameter = new Event.Parameter();
                parameter.parameterID = ParameterID.valueOf(parameterId);
                parameter.value = parameterValue;
                event.parameters.add(parameter);
Debug.println(Level.FINE, "parameters: " + parameter);
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
    private List<Event> events = new ArrayList<>();

    /** */
    static class Event {
        /**
         * 0x00 Default Parameter
         * 0x40...0x7f
         */
        int eventType;
        /** */
        List<Parameter> parameters = new ArrayList<>();
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
        /** フォントの種類 */
        FontType(0x01),
        /** フォントのサイズ */
        FontSize(0x02),
        /** 文字並び方向 */
        Direction(0x03),
        /** 文字アトリビュート指定 将来拡張 */
        Attribute(0x04),
        /** 文字色 */
        FontColor0(0x10),
        /** 色替え後文字色 */
        FontColor1(0x11),
        /** 文字縁取り色 将来拡張 */
        EdgeColor0(0x12),
        /** 色替え後文字縁取り色 将来拡張 */
        EdgeColor1(0x13),
        /** 文字背景色 */
        BackColor0(0x14),
        /** 色替え後文字背景色 */
        BackColor1(0x15),
        /** デフォルトの座標指定方法 */
        Coordinates(0x20),
        /** 背景色 Plane 0 指定色 */
        BackDropColor(0x30),
        /** 透明色とする色を指定 */
        TransparentColor(0x31),
        /** 透明処理の有効フラグ */
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

/* */
