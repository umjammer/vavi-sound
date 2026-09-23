# vavi.sound.smd

SMD (J-Phone J-SKY melody, `.smd`, `.smz`) reader and MIDI converter.

 * `SmdReader` ... reads to `Smd`
 * `SmdMidiConverter` ... `Smd` to `javax.sound.midi.Sequence`
 * `vavi.sound.midi.smd.SmdMidiFileReader` ... `MidiSystem.getSequence()` accepts SMD

Reverse engineered from `PsmPlay.exe` (PsmPlayer 5.0, APO), function at `0x41db03`.
An `.smz` is either this or SMAF (`MMMD`, see `vavi.sound.smaf`).

## file

7 bit text, so that it can be sent in a mail.

```
title ... ISO-2022-JP: ESC ( B, ESC $ B (JIS X 0208), SO ... SI (half width kana), other bytes as is (ASCII, shift_jis)
ESC $ D  part 0  SI
ESC $ D  part 1  SI
 :                      up to 16 parts, played at the same time, part n at MIDI channel n
```

## part

resolution is 12 ticks per quarter note.

| byte          | meaning                                                                   |
|---------------|---------------------------------------------------------------------------|
| `#` `$` `%` `&` | rest of 16th, 8th, quarter, half                                        |
| 0x27 ~ 0x7f   | note, key `((c + 0x30) % 0x58) / 4` semitones from A4, length `c & 3`: 16th, 8th, quarter, half |
| `"`           | tie, a rest to the next rest, a note to the next note of the same key     |
| `!"` `!#` `!$` | octave +1, -1, 0                                                         |
| `!%`          | the next note three times as a triplet                                    |
| `!&`          | the next three notes (or rests) as a triplet                              |
| `!(` `!)` `!*` `!+` | tempo 150, 126, 108, 96 bpm, PsmPlay uses the ones of part 0 only   |
| `!,` `!-` `!.` `!/` | volume -2, -1, +2, +1, clamped to -4 ~ 4, MIDI volume `level * 8 + 95` |

a triplet makes a length `length * 2 / 3` (truncated).

## verified

 * all 1496 non SMAF files in `~/Public/np2/smd` (2548 `.smz`, 1051 SMAF, 1 empty) have no other bytes than above,
   and convert to MIDI through `MidiSystem`
 * about 2/3 of them have parts of the same length (within a quarter note), the others end parts earlier
   or have an empty part, which is how they are written

## TODO

 * program, PsmPlay does not set one (the handset's tone)
