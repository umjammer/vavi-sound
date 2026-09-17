/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

import org.junit.jupiter.api.Test;

import vavi.sound.mfi.vavi.VaviMfiSynthesizer.VaviMfiReceiver;
import vavi.sound.smaf.vavi.VaviSmafSynthesizer.VaviSmafReceiver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * The adpcm driver receivers in front of a synthesizer ask it for its receiver once, not
 * once a message: a synthesizer which hands out a new receiver each time keeps them all.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-13 nsano initial version <br>
 */
class ReceiverTest {

    /** a synthesizer which makes a new receiver on every getReceiver, like NukedSynthesizer */
    static class Fake {
        final List<MidiMessage> sent = new ArrayList<>();
        int receivers;
        int closed;

        Synthesizer synthesizer() {
            return (Synthesizer) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Synthesizer.class}, (proxy, method, args) ->
                switch (method.getName()) {
                    case "getReceiver" -> {
                        receivers++;
                        yield new Receiver() {
                            @Override public void send(MidiMessage message, long timeStamp) { sent.add(message); }
                            @Override public void close() { closed++; }
                        };
                    }
                    case "getLatency" -> 0L;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "fake synthesizer";
                    default -> null;
                });
        }
    }

    void test(Function<Synthesizer, Receiver> factory) throws Exception {
        Fake fake = new Fake();
        Receiver receiver = factory.apply(fake.synthesizer());
        for (int i = 0; i < 100; i++) {
            receiver.send(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 100), -1);
        }
        assertEquals(100, fake.sent.size());
        assertEquals(1, fake.receivers, "one receiver for all the messages");

        receiver.close();
        assertEquals(1, fake.closed);
        receiver.send(new ShortMessage(ShortMessage.NOTE_OFF, 0, 60, 0), -1);
        assertTrue(fake.sent.size() == 100, "nothing after close");
    }

    @Test
    void smafReceiver() throws Exception {
        test(VaviSmafReceiver::new);
    }

    @Test
    void mfiReceiver() throws Exception {
        test(VaviMfiReceiver::new);
    }
}
