/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * AudioEngineSyncTest.
 * <p>
 * latency compensation scheduling, no real audio device needed.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-03 nsano initial version <br>
 */
class AudioEngineSyncTest {

    @AfterEach
    void teardown() {
        AudioEngine.Sync.setSynthesizerLatency(0);
    }

    @Test
    void appliesDelayAndKeepsOrder() throws Exception {
        AudioEngine.Sync.setSynthesizerLatency(150);
        assertEquals(150, AudioEngine.Sync.getDelay());

        long t0 = System.nanoTime();
        CountDownLatch cdl = new CountDownLatch(2);
        List<String> order = new CopyOnWriteArrayList<>();
        AudioEngine.Sync.schedule(() -> { order.add("first"); cdl.countDown(); });
        AudioEngine.Sync.schedule(() -> { order.add("second"); cdl.countDown(); });
        assertTrue(cdl.await(5, TimeUnit.SECONDS), "scheduled tasks did not run");
        long elapsedMillis = (System.nanoTime() - t0) / 1_000_000;

        assertTrue(elapsedMillis >= 150, "ran after " + elapsedMillis + " ms, expected >= 150 ms");
        assertEquals(List.of("first", "second"), order);
    }

    @Test
    void taskExceptionDoesNotKillScheduler() throws Exception {
        AudioEngine.Sync.schedule(() -> { throw new IllegalStateException("boom"); });
        CountDownLatch cdl = new CountDownLatch(1);
        AudioEngine.Sync.schedule(cdl::countDown);
        assertTrue(cdl.await(5, TimeUnit.SECONDS), "scheduler died after task exception");
    }
}
