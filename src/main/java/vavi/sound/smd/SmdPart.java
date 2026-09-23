/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smd;

import java.util.List;


/**
 * A part of SMD, {@code ESC $ D} ... {@code SI}. Parts are played at the same time.
 *
 * @param number 0 origin, PsmPlay plays it at MIDI channel {@code number}
 * @param events events in the part
 * @param truncated true when the data ended before {@code SI}
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public record SmdPart(int number, List<SmdEvent> events, boolean truncated) {

    /** @return the end time of the part */
    public long length() {
        if (events.isEmpty()) {
            return 0;
        }
        return switch (events.getLast()) {
        case SmdEvent.NoteEvent n -> n.tick() + n.length();
        case SmdEvent.RestEvent r -> r.tick() + r.length();
        case SmdEvent e -> e.tick();
        };
    }
}
