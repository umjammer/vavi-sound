# vavi.sound.mfm

MFM (FueTrek MFMP, `mfmp`, au) reader and MIDI converter.

 * `MfmReader` ... reads to `Mfm`
 * `MfmMidiConverter` ... `Mfm` to `javax.sound.midi.Sequence`
 * `vavi.sound.midi.mfm.MfmMidiFileReader` ... `MidiSystem.getSequence()` accepts MFM

There is no public spec. This is reverse engineered from `rt_parser_std.dll` of Faith Ring Tone Authoring Tool 1.5.0
(`~/.wine/drive_c/Program Files (x86)/Faith/Ring Tone Authoring Tool/Tools`, it also reads `melo` and `cmid`),
and checked by converting made up SMFs with `rt_smf2mfmp.exe` (runs on wine).
MFMP is a relative of MFi: the same chunk layout, the same 1 byte delta + status stream, with its own commands.

## file

```
"mfmp"
u32   length (following bytes)
u8    major type (1)
u8    minor type (0)
u8    0
u8    number of chunks after the header
u16   header length
sub chunks ... tag (4 bytes), u16 length, data
chunks ... tag (4 bytes), u32 length, data
```

### sub chunks

| tag    | data               | rt_parser_std.dll                                          |
|--------|--------------------|------------------------------------------------------------|
| `titl` | title              |                                                            |
| `copy` | copyright          |                                                            |
| `date` | date               |                                                            |
| `sorc` | protection         |                                                            |
| `supt` | "1100", "1.0.0.0"  |                                                            |
| `note` | u16                | extra bytes of a note, 1: velocity byte, 0: compact mode   |
| `exta` | u16                | extra data length of class 0x7f commands 0x00 ~ 0x7f       |
| `extb` | u16                | extra data length of class 0xbf commands 0x00 ~ 0x7f       |
| `extc` | u16                | extra data length of class 0xff commands 0x00 ~ 0x7f       |
| `tmbs` | u16                | ticks per quarter note, 0 or none: 48                      |
| `cuep` | u32, u32           | cue points                                                 |
| `ainf` | (id, length, value)... | audio information, e.g. sampling rates                 |

### chunks

| tag    | data                                            |
|--------|-------------------------------------------------|
| `ucs ` | u16 count, (u32 size, FueTrek UCS voice)...     |
| `wave` | u16 count, (u32 size, format, rate, ?, data)... |
| `trac` | events, 4 tracks at most                        |

wave: format 0 ADPCM (ROHM original), 2 ADPCM (G.726), rate 4: 4kHz, 5: 8kHz (for Type 1 and 5 players,
the others use other values)

## track

`[delta (1 byte), event]...`

| event                          | bytes                                  |                                                  |
|--------------------------------|----------------------------------------|--------------------------------------------------|
| note                           | `vvkkkkkk gate [VVVVVVbb]`             | key `k + (45, 65, 0, 0)[b]`, velocity `V * 2` (126 in compact mode) |
| pitch bend (class 0x3f)        | `0x3f vvhhhhhh llllllll`               | 14 bit                                           |
| class 0x7f, 0xbf, 0xff         | `status command data`                  | command 0x00 ~ 0x7f: 2 + ext? bytes, 0x80 ~ 0xef: 1 byte, 0xf0 ~ 0xff: u16 length + data |

channel = `track * 4 + voice`, the data of 0xd# commands is `vv` voice + 6 bit value.

| 0xff   | meaning                                                               |
|--------|-----------------------------------------------------------------------|
| 00~03  | gate extension, voice = command, data `bbkkkkkk gate` (from now)      |
| b0     | time skip, `data * 256`                                               |
| b1     | end of track                                                          |
| bf     | tempo, bpm `data + 20`                                                |
| c0     | master volume `data / 2` (track 0)                                    |
| c1     | master balance `data / 2` (track 0)                                   |
| c2     | master coarse tuning `data - 0x40` (track 0)                          |
| c3, c4 | track 0, not known                                                    |
| d0     | bank 1, 1: rhythm                                                     |
| d1     | bank 2, bit 0: program + 64, 0x34 (from GM2 bank 125), 0x36           |
| d2     | program change, bank select MSB: 0x79 melody, 0x78 rhythm, 0x7d, 0x14, 0x11 by d0/d1 |
| d3     | volume `* 2` (the converter folds expression into this)               |
| d4     | pan pot `* 2`                                                         |
| d5     | pitch bend range                                                      |
| d6     | modulation `* 2`                                                      |

| 0x7f   | meaning                                                               |
|--------|-----------------------------------------------------------------------|
| 00     | audio play, data `vvVVVVVV wave`                                      |
| d0     | audio volume `* 2`                                                    |
| d1     | audio pan pot `* 2`                                                   |
| d8~da  | track 0, not known                                                    |
| f0     | 4 bytes, loads the UCS voice of the index                             |
| f1     | `00 xx yy`, not known                                                 |

class 0xbf is skipped by the dll.

## verified

 * all 6 samples parse to end of track at the end of every track
 * notes of `Type2_SMF2MFMP_SAMPLE.mfm` are the same (time, channel, key) as its source `Type2_SMF2MFMP_SAMPLE.mid`

## TODO

 * decoding waves (ROHM ADPCM, G.726 exist in `vavi.sound.adpcm`), UCS voices
