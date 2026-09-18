/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import vavi.sound.mfi.vavi.AudioDataChunk.AudioDataMessage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * AudioDataChunkTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-18 nsano initial version <br>
 */
class AudioDataChunkTest {

    /**
     * "n703id/03 FRIENDS.mld" starts its adpcm with 0x7e, which was taken as Δ 126,
     * the wave came after the play of it at Δ 0 and the whole song was silent.
     */
    @Test
    void noDelta() throws Exception {
        byte[] adpcm = {0x7e, 0x54, (byte) 0xe1, 0x18};
        byte[] adpm = {'a', 'd', 'p', 'm', 0, 3, 11, 4, 1};

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeBytes(AudioDataChunk.TYPE);
        int headerLength = 1 + 1 + adpm.length; // format + attribute + sub chunks
        dos.writeInt(2 + headerLength + adpcm.length);
        dos.writeShort(headerLength);
        dos.writeByte(0x82); // format
        dos.writeByte(0x03); // attribute
        dos.write(adpm);
        dos.write(adpcm);

        AudioDataChunk chunk = new AudioDataChunk(0);
        chunk.readFrom(new ByteArrayInputStream(baos.toByteArray()));
        AudioDataMessage message = chunk.getAudioDataMessage();

        assertEquals(0x7e, message.getData()[0]);
        assertEquals(0, message.getDelta());
    }
}
