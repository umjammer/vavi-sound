# vavi.sound.pmd

PMD (au, Qualcomm CMX, `cmid`) reader and MIDI converter.

 * `PmdReader` ... reads to `Pmd`
 * `PmdMidiConverter` ... `Pmd` to `javax.sound.midi.Sequence`
 * `vavi.sound.midi.pmd.PmdMidiFileReader` ... `MidiSystem.getSequence()` accepts PMD

Reverse engineered from `PsmPlay.exe` (PsmPlayer 5.0, APO), function at `0x4174ee`.
It reads `cmid` with the same routine as MFi `melo`, a PMD file is an MFi file
with another magic and some extensions.

## file

```
"cmid"
u32   length (following bytes)
u16   header length (following 3 bytes + sub chunks)
u8    major type ... 2
u8    contents type ... bit 0: song, bit 1: wave (see "cnts")
u8    number of tracks
sub chunks ... tag (4 bytes), u16 length, data
"trac" u32 length, events ...
 :
```

### sub chunks

| tag    | data                           | PsmPlay                                  |
|--------|--------------------------------|------------------------------------------|
| `vers` | "0500"                         | version = digits                         |
| `sorc` | 1 byte                         | bit 0: copyright                         |
| `date` | "20031224"                     | shown                                    |
| `titl` | title (shift_jis)              | shown                                    |
| `copy` | copyright                      | shown                                    |
| `prot` | protection                     | shown                                    |
| `note` | 2 bytes                        | byte 1 != 0: note events are 3 bytes     |
| `code` | 1 byte                         |                                          |
| `wave` | 1 byte                         |                                          |
| `poly` | 1 byte                         |                                          |
| `tool` | "3.1.297"                      |                                          |
| `exsn`, `exsa`, `exsb`, `exsc` | 2 bytes |                                          |
| `pcpi` | 1 byte                         |                                          |
| `cnts` | "SONG", "SONG;WAVE"            |                                          |

## track

`[delta (1 byte), event]...`, the last event is end of track (`0xff 0xdf 0x00`).

| event                            | bytes                                | |
|----------------------------------|--------------------------------------|-|
| note                             | `vvkkkkkk gate [VVVVVVoo]`           | v: voice, k: key (0 = MIDI 45), V: velocity, o: octave shift (0, 1, -2, -1) |
| control                          | `0xff status data`, status < 0xf0    | the same as MFi (see `vavi.sound.mfi.vavi.track`) |
| wide pitch bend (CMX)            | `0xff 0vvhhhhh llllllll`             | 13 bit, center 0x1000, **PsmPlay ignores it** |
| long                             | `0xff status u16:length data`        | status >= 0xf0 |
| wave (CMX)                       | `0xff 0xf1 u16:length data`          | embedded wave of "SONG;WAVE", body unknown |

 * channel = `track * 4 + voice` until channel assign (`0xff 0xe5 vv00cccc`)
 * tempo `0xff 0xc# bpm`, `#` is the timebase: 6, 12, 24, 48, 96, 192, 384, -, 15, 30, 60, 120, 240, 480, 960, -
 * `0xff 0xe#`: data is `vv` voice + 6 bit value, PsmPlay doubles the value for MIDI
 * `0xff 0xe0`/`0xe1` program/bank, PsmPlay makes a GM program by
   `(p & 0x3f) + ((b & 1) << 6) + ((b & 0xfe) << 7)`, except for bank 0 and 1,
   which have only presets 0 ~ 5 (GM 0, 9, 16, 24, 13, 74), other programs there become 0

## verified

 * all 19 `~/Public/np2/SPH-A920/*.pmd` parse to the end of every track with end of track
 * notes of `Converted/*.mid` (PsmPlayer 3.80 output, later edited) are found at the same time/key
   in 16 of 19 files (the output has the leading rest cut)

## MIDI

 * resolution: the timebase of the first tempo, ticks are not rescaled
 * channel: `track * 4 + voice` and channel assign, bank 63 is a drum kit (it is at channel 9 in the corpus,
   GM2 rhythm bank select is added for other channels)
 * wide pitch bend is converted, wave events are dropped

verified against `Converted/*.mid`: programs of the melodic channels are the same in 13 of 19,
the others are the "SONG;WAVE" files, the MIDI of which has sound effect channels added by hand instead of the wave,
and one program change moved to the top

## TODO

 * body of the wave event
