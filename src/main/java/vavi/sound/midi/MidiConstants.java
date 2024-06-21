/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Properties;

import static java.lang.System.getLogger;


/**
 * Constants for Midi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020703 nsano initial version <br>
 */
public final class MidiConstants {

    private static final Logger logger = getLogger(MidiConstants.class.getName());

    private MidiConstants() {
    }

    /** instrument names */
    private static final Properties props = new Properties();

    /** */
    public static String getInstrumentName(int index) {
        return props.getProperty("midi.inst.gm." + index);
    }

    //-------------------------------------------------------------------------

    // Meta Event
    //
    // Here, some meta-events are defined.
    // Not all programs have to support all meta events.
    // The first meta-events defined are

    public enum MetaEvent {
        /**
         * <pre>
         * FF 00 02 ssss  [sequence number]
         * </pre>
         * This optional event must be placed at the beginning of the track, before any non-zero delta time,
         * and before any transportable MIDI event, which specifies the number of the sequence.
         * The numbers in this track correspond to the sequence numbers in the new Cue messages discussed
         * at the Summer 1987 MMA meeting. This is used in Format 2 MIDI files to identify each "pattern"
         * so that "song" sequences using cue messages can reference the patterns.
         * If the ID number is omitted, the sequence's position in the file is used as the default.
         * In a format 0 or 1 MIDI file, there is only one sequence, so this number will be on the first
         * (and thus only) track. If the transfer of several multi-track sequences is required,
         * it must be done as a group of format 1 files, each with a different sequence number.
         */
        META_SEQUENCE_NO(0x00),

        /**
         * <pre>
         * FF 01 len text  [text event]
         * </pre>
         * Text of any size and content. It's a good idea to write the track name, intended orchestration,
         * and any other information you want there at the beginning of the track. Text events can also be
         * included at other times in a track to describe lyrics or cue points. The text in this event
         * must be printable ASCII characters to ensure maximum compatibility. However, other character codes
         * that use high-order bits can also be used to exchange files between different programs on the same computer
         * that support extended character sets. Can be used. Programs on machines that do not support non-ASCII
         * characters must ignore such codes.
         * <p>
         * (Added in 0.06) Meta event types 01 through 0F are reserved for various types of text events.
         * Each of these overlaps with the text event characteristics described above, but is used for
         * different purposes, as follows:
         */
        META_TEXT_EVENT(0x01),

        /**
         * <pre>
         * FF 02 len text  [show copyright]
         * </pre>
         * Contains the copyright notice as printable ASCII text. This notice must include the letter (C),
         * the year of publication of the work, and the name of the copyright owner. When you have several
         * pieces of music in one MIDI file, you must place all copyright notices in this event so that
         * they appear at the beginning of the file. This event must be placed as the first event in the
         * first track block with delta time = 0.
         */
        META_COPYRIGHT(0x02),

        /**
         * <pre>
         * FF 03 len text  [sequence name or track name]
         * </pre>
         * For format 0 tracks or the first track of a format 1 file, the name of the sequence.
         * Otherwise, the name of the track.
         */
        META_NAME(0x03),

        /**
         * <pre>
         * FF 04 len text  [instrument name]
         * </pre>
         * Describes the type of instrumentation that should be used on the track. It may also be used in
         * conjunction with a meta-event placed at the beginning of a MIDI statement to specify which
         * MIDI channel the description applies to. Alternatively, the channel may be identified by text in this event.
         */
        META_INSTRUMENT(0x04),

        /**
         * <pre>
         * FF 05 len text  [lyrics]
         * </pre>
         * lyrics. Generally, each syllable is an independent lyric event starting at the time of that event.
         */
        META_LYRICS(0x05),

        /**
         * <pre>
         * FF 06 len text  [marker]
         * </pre>
         * Usually located on a format 0 track or the first track of a format 1 file. The name of the current point
         * in the sequence, such as a rehearsal mark or section name. (“First Verse” etc.)
         */
        META_MARKER(0x06),

        /**
         * <pre>
         * FF 07 len text  [queue point]
         * </pre>
         * A description of what is happening on film, on a video screen, or on stage at that point in the score.
         * (“A car crashes into a house,” “The curtain opens,” “A woman makes a man slap her,” etc.)
         */
        META_QUE_POINT(0x07),

        /**
         * <pre>
         * FF 2F 00  [end of track]
         * </pre>
         * This event cannot be omitted. This ensures that the correct end of the track is clear and the track
         * has the correct length. This is necessary when tracks are looped or concatenated.
         */
        META_END_OF_TRACK(0x2f), // 47

        /**
         * <pre>
         * FF 51 03 tttttt  [tempo (unit is μsec / MIDI quoter note)]
         * </pre>
         * This event indicates a tempo change. In other words, "μsec / MIDI quarter note" is "1/24 of
         * (μsec / MIDI clock)". By giving tempo in terms of time/beats rather than beats/time,
         * you can achieve absolutely accurate long-term synchronization using real-time based synchronization
         * protocols such as SMPTE time code or MIDI time code. Obtainable. The accuracy achieved with this tempo
         * setting is such that at 120 beats per minute he can complete a 4 minute song with an error of less
         * than 500 microseconds. Ideally, these events should only be placed on cues where there is a MIDI clock.
         * This is intended to ensure, or at least increase the likelihood of, compatibility with other synchronizing
         * devices, so that time signatures and tempo maps saved in this format can easily be transferred to other
         * devices. It will be possible to transfer.
         */
        META_TEMPO(0x51), // 81

        /**
         * <pre>
         * FF 54 05 hr mn se fr ff  [SMPTE offset (added 0.06 - SMPTE format description)]
         * </pre>
         * This event indicates the SMPTE time, if any, that the track block is to start. This must be placed at
         * the beginning of the track. That is, before any non-zero delta time and before any transportable MIDI
         * event. Time must be encoded in SMPTE format, just like MIDI time code. In format 1 files, SMPTE offsets
         * must be stored with the tempo map and have no meaning on other tracks. Even in SMPTE-based tracks that
         * specify different frame resolutions for delta time, the ff field has subdivided frames
         * (in hundredths of a frame).
         */
        META_SMPTE_OFFSERT(0x54), // 84

        /**
         * <pre>
         * FF 58 04 nn dd cc bb  [time signature]
         * </pre>
         * A time signature is represented by four numbers. nn and dd represent the numerator and denominator of the
         * time signature, as in notation. The denominator is 2 to the negative power. That is, 2 represents a quarter
         * note, 3 represents an eighth note, and so on. The parameter cc represents the number of MIDI clocks per
         * metronome click. The parameter bb represents how many notated thirty-second notes fit into a MIDI quarter
         * note (24 MIDI clocks). This parameter was added because there are already a number of programs that allow
         * the user to define MIDI quarter notes (24 clocks) to be notated as something else, or to correspond
         * expressively to other notes.
         * <p>
         * Therefore, in 6/8 time, the metronome ticks every third eighth note, but with 24 quarter note clocks and
         * 72 clocks per measure, the time signature in hexadecimal is as follows:
         * <p>
         * FF 58 04 06 03 24 08
         * <p>
         * This is a 6/8 time signature (8 is 2 to the power of 3, so 06 03), 36 MIDI clocks per dotted quarter note
         * (24 in hex!) [*2], and a MIDI quarter note. This shows that there are eight thirty-second notes in musical
         * notation.
         */
        META_58(0x58), // 88

        /**
         * <pre>
         * FF 59 02 sf mi  [key signature]
         *   sf = -7  7 flats
         *   sf = -1  1 flat
         *   sf = 0   C key
         *   sf = 1   1 sharp
         *   sf = 7   7 sharps
         *
         *   mi = 0   major key
         *   mi = 1   minor key
         * </pre>
         */
        META_59(0x59), // 89

        /**
         * <pre>
         * FF 7F len data  [sequencer specific meta events]
         * </pre>
         * This event type can be used for special needs for a particular sequencer. The first data byte is the
         * manufacturer ID. However, since this is an interchange format, extending the spec body is preferable
         * to using this event type. This type of event may be used by sequencers that choose to use this as their
         * only file format. For sequencers that have a fixed specification format, the standard specifications
         * should be followed when using this format.
         */
        META_MACHINE_DEPEND(0x7f), // 127
        META_UNDEFINED(-1);
        int number;

        public int number() {
            return number;
        }

        MetaEvent(int number) {
            this.number = number;
        }

        public static MetaEvent valueOf(int number) {
            try {
                return Arrays.stream(values()).filter(e -> e.number == number).findFirst().get();
            } catch (NoSuchElementException e) {
                META_UNDEFINED.number = number; // TODO evil and not thread safe.
                logger.log(Level.WARNING, "undefined meta: " + number);
                return META_UNDEFINED;
            }
        }

        @Override
        public String toString() {
            if (ordinal() == META_UNDEFINED.ordinal()) {
                return super.toString() + " (" + number + ")";
            } else {
                return super.toString();
            }
        }
    }

    //----

    /**
     * 01H ~ 1FH
     * 00H 00H 01H ~ 00H 1FH 7FH
     */
    public static final int SYSEX_MAKER_ID_American = 0x01;

    /**
     * 20H ~ 3FH
     * 00H 20H 00H ~ 00H 3FH 7FH
     */
    public static final int SYSEX_MAKER_ID_European = 0x20;

    /**
     * 40H ~ 5FH
     * 00H 40H 00H ~ 00H 5FH 7FH
     */
    public static final int SYSEX_MAKER_ID_Japanese = 0x40;

    /**
     * 60H ~ 7CH
     * 00H 60H 00H ~ 00H 7FH 7FH
     */
    public static final int SYSEX_MAKER_ID_Other = 0x60;

    /**
     * 7DH ~ 7FH
     */
    public static final int SYSEX_MAKER_ID_Special = 0x70;

    // 7DH
    // Manufacturer ID 7DH is an ID used for research in schools and educational institutions,
    // and can only be used for non-commercial purposes.
    //
    // 7EH
    // 7EH is a non-real-time universal system exclusive. Exclusive is used for data transfer,
    // which is convenient if it can be exchanged across manufacturers and models.
    // Note that 7EH is used for data transfers that are not related to real time.
    //
    // 7FH
    // 7FH is a real-time universal system exclusive. Similar to 7EH's non-real-time universal system exclusive,
    // it is used when exchanging data across manufacturers and models, and in 7FH it is used for data transfer
    // related to real time.

    //----

    /* */
    static {
        try {
            final Class<?> clazz = MidiConstants.class;
            props.load(clazz.getResourceAsStream("midi.properties"));
        } catch (IOException e) {
            logger.log(Level.DEBUG, e);
            throw new IllegalStateException(e);
        }
    }
}
