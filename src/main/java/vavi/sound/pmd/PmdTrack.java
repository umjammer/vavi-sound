/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pmd;

import java.util.List;


/**
 * PMD {@code trac} chunk.
 *
 * @param number 0 origin, in order of appearance
 * @param events events in the track
 * @param truncated true when the data ended in the middle of the track
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-22 nsano initial version <br>
 */
public record PmdTrack(int number, List<PmdEvent> events, boolean truncated) {
}
