/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import vavi.sound.mfi.vavi.SubMessage;


/**
 * ノートメッセージ長情報 MFi Header Sub Chunk.
 *
 * <pre>
 *  &quot;note&quot; 2 bytes: note length (1 for 4byte)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 *          0.01 030907 nsano complete <br>
 */
public class NoteMessage extends SubMessage {

    /** */
    public static final String TYPE = "note";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public NoteMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /** */
    public NoteMessage(int data) {
        super(TYPE, new byte[] {
            (byte) ((data & 0xff00) >> 8),
            (byte)  (data & 0x00ff)
        });
    }

    /** Length of {@link vavi.sound.mfi.NoteMessage} 1: 4byte */
    public int getNoteLength() {
        byte[] data = getData();
//Debug.println(data[0] * 0xff + data[1]);
        return data[0] * 0xff + data[1];
    }

    /** */
    public void setNoteLength(int noteLength) {
        this.data[4] = (byte) ((noteLength & 0xff00) >> 8);
        this.data[5] = (byte)  (noteLength & 0x00ff);
    }

    /** */
    public String toString() {
        return "note: " + getDataLength() + ": " + getNoteLength();
    }
}

/* */
