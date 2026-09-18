/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sub;

import vavi.sound.mfi.vavi.MidiContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * AinfChunkTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-19 nsano initial version <br>
 */
class AinfChunkTest {

    /** audio chunks told but no audio info, as F901iC files have */
    @Test
    void noAudioInfo() throws Exception {
        AinfChunk chunk = (AinfChunk) new AinfChunk().init(AinfChunk.TYPE, new byte[] { 0x04, 0x00 });
        assertEquals(4, chunk.getAudioChunksCount());
        assertEquals(0, chunk.getAudioInfoCount());
        assertEquals(0, chunk.getMidiEvents(new MidiContext()).length);
    }

    @Test
    void audioInfo() throws Exception {
        AinfChunk chunk = (AinfChunk) new AinfChunk().init(false, 1, new AinfChunk.AudioInfo(0x81, new byte[] { 0x10, 0x08 }));
        assertEquals(1, chunk.getMidiEvents(new MidiContext()).length);
    }
}
