# vavi.sound.mfi.vavi.nec

Provides NEC-specific message processing classes.

A NEC machine dependent message is a `0xff 0xff` sysex whose payload starts with the
vendor byte `0x11` (`VENDOR_NEC | CARRIER_DOCOMO`). The byte after the vendor byte
selects the *message level*:

```
 ff ff <len:2> 11 <level> <group> <function> <payload...>
                     |      |        |
                     |      |        +-- 0x0_ (plus flags in the upper nibble)
                     |      +----------- 0xf0 .. 0xf3
                     +------------------ 0x01 = MFi 3.0 (MA-3/MA-5), 0x02 = MFi 4.0 (MA-7)
```

When the level byte is neither `0x01` nor `0x02` there is no level byte at all -
the two bytes after the vendor are `<group>` and `<function>|<channel>` (the
"Level 0x00" list below). `NecSequencer` builds the lookup key from those bytes,
`f1_f2_f3` for level 1 / 2 and `f1_f2` otherwise.

## Level 0x00 (plain, 2 byte header)

- f0._1 FM voice change
- f0._2 ADPCM data
- f1._0 ADPCM on
- f1._1 Vibrato
- f1._2 pitch bend
- f2._1 ADPCM volume
- f2._4 vibrator
- f2._5 LED
- f2._6 ? (TODO more investigation, a device control in the same group - same
  two byte payload and value space as vibrator and LED, 3 messages in the whole
  corpus)

## Level 0x01 - MFi 3.0 (MA-3 / MA-5)

| id | payload | meaning |
|---|---|---|
| `01.f0._3` | n | Extended FM basic waveform |
| `01.f0._4` | n * record | **Extended FM tone specification** - see below |
| `01.f0._5` | n * 18 | **Extended WT tone specification** - see below |
| `01.f0._6` | 2 + n | **Extended WT waveform specification** - see below |
| `01.f0._7` | n | Extended stream waveform control information |
| `01.f0._8` | n * record | **Extended AL tone specification** - see below |
| `01.f1.x3` | 2 | StreamOn |
| `01.f1.x4` | 2 | StreamSlaveOn |
| `01.f1.x5` | 1 | StreamOff |
| `01.f1.x6` | 2 | StreamPan |
| `01.f1.x7` | 1 | Hold1 |
| `01.f1.x8` | 0 | MonoOn (no data at all, the message is 4 bytes) |
| `01.f1.x9` | 0 | PlayOn (no data) |
| `01.f1.xa` | 1 | filter resonance, 0 ~ 127 (64 neutral) |
| `01.f1.xb` | 1 | filter brightness, 0 ~ 127 (64 neutral) |
| `01.f2._7` | 16 | Channel Status control information - see below |
| `01.f3._1` | 1 | FM mode setting |
| `01.f3._2` | n | UserEvent |
| `01.f3._3` | 1 | MaxGain setting |
| `01.f3._4` | 1 | Specify number of streams |
| `01.f3._5` | n | AL channel specification |
| `01.f3._7` | 1 | ? (0 or 3) |
| `01.f3._a` | 1 | ? (always 1, looks like the same thing as `02.f3.0a`) |

### the tone specification messages

`01.f0._4`, `_5` and `_8` all register voices, one or more per message, as

```
 [type] bank program <voice ...>
```

`bank` matches the `ChangeBank` (`0xe1`) data of the channel that plays the voice
(bit 7 marks a drum voice) and `program` the `ChangeVoice` (`0xe0`) data, exactly
like the MA-7 `02.f0.0c` header. Checked over `~/Public/np2/mfi`: the bank field
lands on a bank the file actually selects for 5892 / 5892 FM records, 5167 / 5167
WT records and 77 / 77 AL records.

The voice bytes are the SMAF/MA-5 voice **without its leading flags byte**, i.e.
the `src` column of the tables further down (the MA-7 message carries the
expanded `dst` side instead). The type byte is only present when the message can
carry more than one voice shape:

| message | type | record | voice | shape |
|---|---|---|---|---|
| `01.f0._4` | 1 | 20 | 17 | FM 2 operator |
| `01.f0._4` | 2 | 34 | 31 | FM 4 operator |
| `01.f0._5` | - | 18 | 16 | WT |
| `01.f0._8` | 1 | 46 | 43 | WT + AL |
| `01.f0._8` | 0 | 47 | 44 | FM 2 operator + AL |
| `01.f0._8` | 0 | 61 | 58 | FM 4 operator + AL |

- the type byte is the **only** reliable way to tell 2 from 4 operator apart.
  104 messages in the corpus are 4 operator voices whose `ALG` is below 2, so do
  not derive the record length from `ALG`. With the type byte all 1407 `_4`
  messages split exactly, 161 of them mixing 2 and 4 operator records.
- `01.f0._8` type 0 covers both FM shapes; every `_8` message in the corpus
  carries exactly one voice, so the message length settles 2 vs 4 operator.
- the FM voice is `KeyNumber`, `Panpot,BO`, `LFO,PE,ALG`, then 7 bytes per
  operator (`SR,XOF,SUS,KSR` / `RR,DR` / `AR,SL` / `TL,KSL` / `DAM,EAM,DVB,EVB` /
  `MULTI,DT` / `WS,FB`).
- the WT voice is the 16 byte VM35 PCM voice, the same bytes a SMAF `EXVO`
  `43 05 02` exclusive carries (`vavi.sound.smaf.chunk.ExclusiveVoiceChunk`).
  All 5167 records in the corpus decode to a sampling rate inside 1 ~ 48000 Hz,
  peaking at 8000 / 12000 / 15000 / 13200 / 10000. Its two bytes that
  `ExclusiveVoiceChunk` still calls "?" (+9, +10) are the
  **StartAddressOffset** MSB / LSB, which the MA-7 conversion table names.

### `01.f0._6` Extended WT waveform specification

```
 11 01 f0 06 <wave id> <format> <wave data ...>
```

`wave id` is what the `RM, WaveID` byte of a WT voice refers to when its RM bit
is 0. `format` is 0 for 4 bit ADPCM (2 samples per byte) and 2 for 8 bit PCM
(1 sample per byte); 3 also occurs 5 times and is unidentified. The voice's
`EndPoint` is the sample count: over the 2055 wave / voice pairs in the corpus
the ratio `EndPoint / dataLength` is 2.0 for every ADPCM wave and 1.0 for every
`format` 2 wave, give or take voices that stop before the end of their wave.

### `01.f2._7` Channel Status control information

16 bytes, one per channel, which is exactly the 4 tracks * 4 channels an MFi 3.0
file addresses. Unlike the MA-7 message the bytes are **not** rotated - the
converter copies the SMAF channel status bytes straight through
(`CnvMA5MFi_N.dll` at `0x10017e93`, source length checked to be 20 and 16 bytes
copied out of it), so they are `KCS:7-6, vibration:5, LED:4, type:1-0` as
`vavi.sound.smaf.chunk.ChannelStatus` reads them.

## Level 0x81 - level 0x01 with bit 7 set

**TODO more investigation.** Exactly one file of the ~4400 in
`~/Public/np2/mfi` writes these (`威風堂々　クラシカル.mld`, MFi 0301), 5 messages:

```
 11 81 f0 04 02 04 30 00 79 87 ...   a 4 operator FM tone for bank 4, program 0x30
 11 81 f0 05 84 00 27 10 79 00 ...   a 10000Hz WT tone for bank 4 (drum), program 0
 11 81 f2 07 00 00 ...               16 bytes of channel status
 11 81 f3 01 00                      FM mode setting
 11 81 f3 03 08                      MaxGain setting
```

Everything from the `f2` byte on is a well formed level 0x01 message, so bit 7 of
the level byte is either a flag that one authoring tool sets or simply a bad
byte. `Function129_0 / _2 / _3` mask it off and hand the message to the level
0x01 function (none of those look at the level byte, so they decode it as is).
`NecSequencer` itself is deliberately left alone - one file is not enough to
start masking the level byte for everybody, and doing it there would also change
how a genuine level 0 message with bit 7 set is routed.

Note the key `NecSequencer` builds for these is `129_<low nibble of the f2 byte>`,
so one class covers a whole `0xf_` group: `129_0` is both `f0.04` and `f0.05`,
`129_3` is both `f3.01` and `f3.03`.

## Level 0x02 - MFi 4.0 (MA-7, N902i and later)

Reverse engineered from `tmp/SCP-MA-N-210-j` (Yamaha *SCP-MA7-N*, the official
SMAF/MA-7 -> MFi4.0(NEC) converter) plus the `tmp/samples/n703id` mld files.
The interesting binary is `Bin/Plugin_SM7_N40/CnvMA7MFi_N.dll`; the message
encoder is the big `switch` at `0x10027960` which turns an internal event id into
the `<group> <function>` pair, so the table below is the converter's complete
level-2 vocabulary.

| id        | internal event | payload | meaning |
|-----------|----------------|---------|---------|
| `02.f0.03` | 200  | n | FM basic waveform (<= 1024 samples) |
| `02.f0.04` | 201  | n | ? |
| `02.f0.05` | 202  | n | ? |
| `02.f0.06` | 203 / 3105 | n | ADPCM (audio) data - `Function2_240_6` |
| `02.f0.08` | 204  | n | ? |
| `02.f0.0a` | 206  | n | ? |
| `02.f0.0b` | 207  | n | ? |
| `02.f0.0c` | 3104 | 4 + voice | **tone (voice) setting** - see below |
| `02.f0.0e` | 3112 | n * 32 (n <= 32) | effect (SFX) parameter block A |
| `02.f0.0f` | 3113 | n * 32 (n <= 32) | effect (SFX) parameter block B |
| `02.f1.x7` | 209  | 1 | Hold1 (same numbering as level 1) |
| `02.f1.x8` | 210  | 1 | MonoOn (one data byte, unlike level 1 which has none) |
| `02.f1.x9` | 211  | 1 | PlayOn (one data byte, unlike level 1 which has none) |
| `02.f1.xa` | 212  | 1 | filter resonance, 0 ~ 127 (64 neutral) |
| `02.f1.xb` | 213  | 1 | filter brightness, 0 ~ 127 (64 neutral) |
| `02.f1.xc` | 214  | 1 | ? (level 2 only, no level 1 counterpart) |
| `02.f1.xd` | 1110 | 1 | channel level / SendLevel #1 |
| `02.f1.xe` | 1111 | 1 | SendLevel #2 |
| `02.f1.xf` | 1112 | 1 | SendLevel #3 |
| `02.f2.07` | 3103 | 32 | Channel Status control information - see below |
| `02.f3.01` | 216  | n | ? |
| `02.f3.02` | 217  | n | ? |
| `02.f3.03` | 218 / 3124 | 1 (or 2) | MaxGain setting |
| `02.f3.07` | 219  | n | ? |
| `02.f3.09` | 205  | n | ? |
| `02.f3.0a` | 220  | 1 | emitted once at the head of track 0, always `01` |
| `02.f3.0b` | 3126 | 2 | SfxChange (`<id> 0xf7`, id 0..31 or 64..95) |

Rows marked `?` are reachable in the encoder but neither the `n703id` samples nor
the ~4400 file `~/Public/np2/mfi` corpus use them, so only the id is known.

### channel field of `02.f1.xx`

The function byte is `<channel> <flags> <function>`:

```
 7 6 5 4 3 2 1 0
 c c f . n n n n
 | | |    +------ function (7..f)
 | | +----------- extra flag, see below
 +-+------------- channel 0..3 inside the track
```

The converter only ever writes `channel << 6 | function` (bit 5 clear), but real
files also contain `0x2d / 0xad / 0xed`, i.e. bit 5 set. In the MFi reader of the
same DLL that bit selects a different SMAF event when converting back (`0x4b6`
instead of `0x456`), so it is a real variant flag and not a wider channel field.
Scanning ~4400 files agrees: the top three bits of the function byte are
`0, 2, 4, 6` almost always (= channel in bits 7-6, bit 5 clear), bit 5 is set in
36 messages out of 1408 and only ever together with channel 0 and only on
`xd / xe / xf` - a 3 bit channel would also produce `3, 5, 7`, which never
happens.

### `02.f2.07` Channel Status control information

32 bytes, one per MA-7 channel. Each byte is the SMAF channel status byte
**rotated left by 2**:

```java
mfi = ((smaf << 2) | (smaf >>> 6)) & 0xff;
```

(see `CnvMA7MFi_N.dll` `0x1000bde0`; the input length must be exactly 32).
With the `MobileStandard` channel status layout of
`vavi.sound.smaf.chunk.ChannelStatus` (`KCS:7-6, vibration:5, LED:4, type:1-0`)
the MFi byte therefore is `vibration:7, LED:6, -:5-4, type:3-2, KCS:1-0`.

The level 1 message (`01.f2._7`) is the same thing with **16** bytes, which is
exactly the 4 tracks * 4 channels an MFi 3.0 file addresses. Level 2 doubles the
table to 32 although the channel field stays 2 bits wide, so the upper half is
either a second bank or simply the MA-7's own 32 channel table copied through.
SCP-MA7-N writes `0x10` for the channels a song does not use, but other encoders
in the corpus fill all 32 entries, so do not read `0x10` as "unused" in general.

### `02.f0.0c` tone (voice) setting

```
 11 02 f0 0c <bank> <program> <c> <d> <voice...>
```

| byte | meaning |
|---|---|
| `bank` | bank number; the low 6 bits match the `ChangeBank` (`0xe1`) data of the channel that uses it, bit 7 = drum/rhythm voice |
| `program` | program number (matches `ChangeVoice` (`0xe0`) data); for a drum voice this is the drum index, i.e. note - 35 |
| `c` | drum voice: the MIDI note number (`program + 35`). melody voice: `0` for a single region voice, otherwise a per split region value (`0x80 0xb5 0xba 0xce 0xde` in `14 Piano.mld`) |
| `d` | upper key limit of the split region (ascending, last one `0x7f`); `0` when the voice has no split |

`voice[0]` is a type/flag byte and decides the length of the rest:

| `voice[0]` | type | voice length |
|---|---|---|
| `0x00` | FM 2 operator | 24 |
| `0x00` | FM 4 operator | 44 |
| `0x01` | WT (wave table) | 18 |
| `0x02` | FM 2 operator + AL (filter) | 40 |
| `0x02` | FM 4 operator + AL | 60 |
| `0x03` | WT + AL | 34 |

- bit 0 = WT (else FM), bit 1 = AL (filter) section present.
- 2 operator and 4 operator share the same flag byte, tell them apart by the
  message length (or by `ALG` in `voice[3]`).
- `0x05` and `0x07` also occur, 4 times in ~4400 files (once `0x05`, three times
  `0x07`). All four carry a 43 byte voice, that is a WT+AL voice (34) followed by
  9 unidentified bytes, and the 4 byte header still follows the rules above
  (three of them are drum voices whose `c` is `program + 35`). The converter
  cannot produce them, so they come from the MA-7 specific path.

The following field maps are the exact conversion tables of
`CnvMA7MFi_N.dll` (`0x1003e5f8`, 25 byte records `{id, name, flag, srcSize,
dstSize, table, count}`, each entry `{srcIndex, srcMask, convFn, dstIndex,
dstMask, shift, name}`, 22 bytes). "from MA-7 voice byte.mask" is the source
byte in the SMAF/MA-7 voice chunk; `(via fn)` means the value is computed, not
copied. Bytes not listed are always 0.

#### FM 2 operator (`MA5_FM2Op`) (voice byte 0 = 0x00, 24 bytes)

| byte | mask | field | from MA-7 voice byte.mask |
|---|---|---|---|
| 0 | 0xff | type flags (0x00) | byte 0 |
| 1 | 0x7f | KeyNumber | 1.0x7f |
| 2 | 0xfb | Panpot, BO | 2.0xfb |
| 3 | 0xe7 | LFO, PE, ALG | 3.0xe7 |
| 3 | 0x08 | ALE | 0.0x02 <<2 |
| 4 | 0xfb | SR, XOF, SUS, KSR | 4.0xfb |
| 5 | 0xff | RR, DR | 5.0xff |
| 6 | 0xff | AR, SL | 6.0xff |
| 7 | 0xff | TL, KSL | 7.0xff |
| 8 | 0x77 | DAM, EAM, DVB, EVB | 8.0x77 |
| 9 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 10 | 0xff | WS, FB | 10.0xff |
| 13 | 0xf0 | MULTI | 9.0xf0 (via fn) |
| 13 | 0x07 | DT | 9.0x07 |
| 14 | 0xfb | SR, XOF, SUS, KSR | 11.0xfb |
| 15 | 0xff | RR, DR | 12.0xff |
| 16 | 0xff | AR, SL | 13.0xff |
| 17 | 0xff | TL, KSL | 14.0xff |
| 18 | 0x77 | DAM, EAM, DVB, EVB | 15.0x77 |
| 19 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 20 | 0xff | WS, FB | 17.0xff |
| 23 | 0xf0 | MULTI | 16.0xf0 (via fn) |
| 23 | 0x07 | DT | 16.0x07 |

#### FM 4 operator (`MA5_FM4Op`) (voice byte 0 = 0x00, 44 bytes)

| byte | mask | field | from MA-7 voice byte.mask |
|---|---|---|---|
| 0 | 0xff | type flags (0x00) | byte 0 |
| 1 | 0x7f | KeyNumber | 1.0x7f |
| 2 | 0xfb | Panpot, BO | 2.0xfb |
| 3 | 0xe7 | LFO, PE, ALG | 3.0xe7 |
| 3 | 0x08 | ALE | 0.0x02 <<2 |
| 4 | 0xfb | SR, XOF, SUS, KSR | 4.0xfb |
| 5 | 0xff | RR, DR | 5.0xff |
| 6 | 0xff | AR, SL | 6.0xff |
| 7 | 0xff | TL, KSL | 7.0xff |
| 8 | 0x77 | DAM, EAM, DVB, EVB | 8.0x77 |
| 9 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 10 | 0xff | WS, FB | 10.0xff |
| 13 | 0xf0 | MULTI | 9.0xf0 (via fn) |
| 13 | 0x07 | DT | 9.0x07 |
| 14 | 0xfb | SR, XOF, SUS, KSR | 11.0xfb |
| 15 | 0xff | RR, DR | 12.0xff |
| 16 | 0xff | AR, SL | 13.0xff |
| 17 | 0xff | TL, KSL | 14.0xff |
| 18 | 0x77 | DAM, EAM, DVB, EVB | 15.0x77 |
| 19 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 20 | 0xff | WS, FB | 17.0xff |
| 23 | 0xf0 | MULTI | 16.0xf0 (via fn) |
| 23 | 0x07 | DT | 16.0x07 |
| 24 | 0xfb | SR, XOF, SUS, KSR | 18.0xfb |
| 25 | 0xff | RR, DR | 19.0xff |
| 26 | 0xff | AR, SL | 20.0xff |
| 27 | 0xff | TL, KSL | 21.0xff |
| 28 | 0x77 | DAM, EAM, DVB, EVB | 22.0x77 |
| 29 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 30 | 0xff | WS, FB | 24.0xff |
| 33 | 0xf0 | MULTI | 23.0xf0 (via fn) |
| 33 | 0x07 | DT | 23.0x07 |
| 34 | 0xfb | SR, XOF, SUS, KSR | 25.0xfb |
| 35 | 0xff | RR, DR | 26.0xff |
| 36 | 0xff | AR, SL | 27.0xff |
| 37 | 0xff | TL, KSL | 28.0xff |
| 38 | 0x77 | DAM, EAM, DVB, EVB | 29.0x77 |
| 39 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 40 | 0xff | WS, FB | 31.0xff |
| 43 | 0xf0 | MULTI | 30.0xf0 (via fn) |
| 43 | 0x07 | DT | 30.0x07 |

#### FM 2 operator + AL (`MA5_FM2OpAL`) (voice byte 0 = 0x02, 40 bytes)

| byte | mask | field | from MA-7 voice byte.mask |
|---|---|---|---|
| 0 | 0xff | type flags (0x02) | byte 0 |
| 1 | 0x7f | KeyNumber | 28.0x7f |
| 2 | 0xfb | Panpot, BO | 29.0xfb |
| 3 | 0xe7 | LFO, PE, ALG | 30.0xe7 |
| 3 | 0x08 | ALE | 0.0x02 <<2 |
| 4 | 0xfb | SR, XOF, SUS, KSR | 31.0xfb |
| 4 | 0x04 | FIX | 18.0x40 >>4 |
| 5 | 0xff | RR, DR | 32.0xff |
| 6 | 0xff | AR, SL | 33.0xff |
| 7 | 0xff | TL, KSL | 34.0xff |
| 8 | 0x77 | DAM, EAM, DVB, EVB | 35.0x77 |
| 9 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 10 | 0xff | WS, FB | 37.0xff |
| 11 | 0x1f | FIXBLOCK, FIXFnum(H) | 18.0x3e >>1 |
| 12 | 0x80 | FIXFnum(L)-1 | 18.0x01 <<7 |
| 12 | 0x7f | FIXFnum(L)-2 | 19.0x7f |
| 13 | 0xf0 | MULTI | 36.0xf0 (via fn) |
| 13 | 0x07 | DT | 36.0x07 |
| 14 | 0xfb | SR, XOF, SUS, KSR | 38.0xfb |
| 14 | 0x04 | FIX | 20.0x40 >>4 |
| 15 | 0xff | RR, DR | 39.0xff |
| 16 | 0xff | AR, SL | 40.0xff |
| 17 | 0xff | TL, KSL | 41.0xff |
| 18 | 0x77 | DAM, EAM, DVB, EVB | 42.0x77 |
| 19 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 20 | 0xff | WS, FB | 44.0xff |
| 21 | 0x1f | FIXBLOCK, FIXFnum(H) | 20.0x3e >>1 |
| 22 | 0x80 | FIXFnum(L)-1 | 20.0x01 <<7 |
| 22 | 0x7f | FIXFnum(L)-2 | 21.0x7f |
| 23 | 0xf0 | MULTI | 43.0xf0 (via fn) |
| 23 | 0x07 | DT | 43.0x07 |
| 24 | 0x1f | Q | 2.0x1f |
| 25 | 0xf0 | FLFO_DEPTH & MODE | 17.0x78 <<1 |
| 25 | 0x08 | FLFO_RST | 26.0x40 >>3 |
| 25 | 0x07 | FLFO_FREQ | 17.0x07 |
| 26 | 0x1f | FC0(H) | 3.0x3e >>1 |
| 27 | 0x80 | FC0(L)-1 | 3.0x01 <<7 |
| 27 | 0x7f | FC0(L)-2 | 4.0x7f |
| 28 | 0x1f | FC1(H) | 5.0x3e >>1 |
| 29 | 0x80 | FC1(L)-1 | 5.0x01 <<7 |
| 29 | 0x7f | FC1(L)-2 | 6.0x7f |
| 30 | 0x1f | FC2(H) | 7.0x3e >>1 |
| 31 | 0x80 | FC2(L)-1 | 7.0x01 <<7 |
| 31 | 0x7f | FC2(L)-2 | 8.0x7f |
| 32 | 0x1f | FC3(H) | 9.0x3e >>1 |
| 33 | 0x80 | FC3(L)-1 | 9.0x01 <<7 |
| 33 | 0x7f | FC3(L)-2 | 10.0x7f |
| 34 | 0x1f | FC4(H) | 11.0x3e >>1 |
| 35 | 0x80 | FC4(L)-1 | 11.0x01 <<7 |
| 35 | 0x7f | FC4(L)-2 | 12.0x7f |
| 36 | 0x80 | FXOF | - (via fn) |
| 36 | 0x1f | FAR | 13.0x1f |
| 37 | 0x80 | FSUS | - (via fn) |
| 37 | 0x1f | FDR | 14.0x1f |
| 38 | 0x80 | FKSL | 27.0x01 <<7 |
| 38 | 0x1f | FSR | 15.0x1f |
| 39 | 0x80 | FVSL | - (via fn) |
| 39 | 0x1f | FRR | 16.0x1f |

#### FM 4 operator + AL (`MA5_FM4OpAL`) (voice byte 0 = 0x02, 60 bytes)

| byte | mask | field | from MA-7 voice byte.mask |
|---|---|---|---|
| 0 | 0xff | type flags (0x02) | byte 0 |
| 1 | 0x7f | KeyNumber | 28.0x7f |
| 2 | 0xfb | Panpot, BO | 29.0xfb |
| 3 | 0xe7 | LFO, PE, ALG | 30.0xe7 |
| 3 | 0x08 | ALE | 0.0x02 <<2 |
| 4 | 0xfb | SR, XOF, SUS, KSR | 31.0xfb |
| 4 | 0x04 | FIX | 18.0x40 >>4 |
| 5 | 0xff | RR, DR | 32.0xff |
| 6 | 0xff | AR, SL | 33.0xff |
| 7 | 0xff | TL, KSL | 34.0xff |
| 8 | 0x77 | DAM, EAM, DVB, EVB | 35.0x77 |
| 9 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 10 | 0xff | WS, FB | 37.0xff |
| 11 | 0x1f | FIXBLOCK, FIXFnum(H) | 18.0x3e >>1 |
| 12 | 0x80 | FIXFnum(L)-1 | 18.0x01 <<7 |
| 12 | 0x7f | FIXFnum(L)-2 | 19.0x7f |
| 13 | 0xf0 | MULTI | 36.0xf0 (via fn) |
| 13 | 0x07 | DT | 36.0x07 |
| 14 | 0xfb | SR, XOF, SUS, KSR | 38.0xfb |
| 14 | 0x04 | FIX | 20.0x40 >>4 |
| 15 | 0xff | RR, DR | 39.0xff |
| 16 | 0xff | AR, SL | 40.0xff |
| 17 | 0xff | TL, KSL | 41.0xff |
| 18 | 0x77 | DAM, EAM, DVB, EVB | 42.0x77 |
| 19 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 20 | 0xff | WS, FB | 44.0xff |
| 21 | 0x1f | FIXBLOCK, FIXFnum(H) | 20.0x3e >>1 |
| 22 | 0x80 | FIXFnum(L)-1 | 20.0x01 <<7 |
| 22 | 0x7f | FIXFnum(L)-2 | 21.0x7f |
| 23 | 0xf0 | MULTI | 43.0xf0 (via fn) |
| 23 | 0x07 | DT | 43.0x07 |
| 24 | 0xfb | SR, XOF, SUS, KSR | 45.0xfb |
| 24 | 0x04 | FIX | 22.0x40 >>4 |
| 25 | 0xff | RR, DR | 46.0xff |
| 26 | 0xff | AR, SL | 47.0xff |
| 27 | 0xff | TL, KSL | 48.0xff |
| 28 | 0x77 | DAM, EAM, DVB, EVB | 49.0x77 |
| 29 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 30 | 0xff | WS, FB | 51.0xff |
| 31 | 0x1f | FIXBLOCK, FIXFnum(H) | 22.0x3e >>1 |
| 32 | 0x80 | FIXFnum(L)-1 | 22.0x01 <<7 |
| 32 | 0x7f | FIXFnum(L)-2 | 23.0x7f |
| 33 | 0xf0 | MULTI | 50.0xf0 (via fn) |
| 33 | 0x07 | DT | 50.0x07 |
| 34 | 0xfb | SR, XOF, SUS, KSR | 52.0xfb |
| 34 | 0x04 | FIX | 24.0x40 >>4 |
| 35 | 0xff | RR, DR | 53.0xff |
| 36 | 0xff | AR, SL | 54.0xff |
| 37 | 0xff | TL, KSL | 55.0xff |
| 38 | 0x77 | DAM, EAM, DVB, EVB | 56.0x77 |
| 39 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 40 | 0xff | WS, FB | 58.0xff |
| 41 | 0x1f | FIXBLOCK, FIXFnum(H) | 24.0x3e >>1 |
| 42 | 0x80 | FIXFnum(L)-1 | 24.0x01 <<7 |
| 42 | 0x7f | FIXFnum(L)-2 | 25.0x7f |
| 43 | 0xf0 | MULTI | 57.0xf0 (via fn) |
| 43 | 0x07 | DT | 57.0x07 |
| 44 | 0x1f | Q | 2.0x1f |
| 45 | 0xf0 | FLFO_DEPTH & MODE | 17.0x78 <<1 |
| 45 | 0x08 | FLFO_RST | 26.0x40 >>3 |
| 45 | 0x07 | FLFO_FREQ | 17.0x07 |
| 46 | 0x1f | FC0(H) | 3.0x3e >>1 |
| 47 | 0x80 | FC0(L)-1 | 3.0x01 <<7 |
| 47 | 0x7f | FC0(L)-2 | 4.0x7f |
| 48 | 0x1f | FC1(H) | 5.0x3e >>1 |
| 49 | 0x80 | FC1(L)-1 | 5.0x01 <<7 |
| 49 | 0x7f | FC1(L)-2 | 6.0x7f |
| 50 | 0x1f | FC2(H) | 7.0x3e >>1 |
| 51 | 0x80 | FC2(L)-1 | 7.0x01 <<7 |
| 51 | 0x7f | FC2(L)-2 | 8.0x7f |
| 52 | 0x1f | FC3(H) | 9.0x3e >>1 |
| 53 | 0x80 | FC3(L)-1 | 9.0x01 <<7 |
| 53 | 0x7f | FC3(L)-2 | 10.0x7f |
| 54 | 0x1f | FC4(H) | 11.0x3e >>1 |
| 55 | 0x80 | FC4(L)-1 | 11.0x01 <<7 |
| 55 | 0x7f | FC4(L)-2 | 12.0x7f |
| 56 | 0x80 | FXOF | - (via fn) |
| 56 | 0x1f | FAR | 13.0x1f |
| 57 | 0x80 | FSUS | - (via fn) |
| 57 | 0x1f | FDR | 14.0x1f |
| 58 | 0x80 | FKSL | 27.0x01 <<7 |
| 58 | 0x1f | FSR | 15.0x1f |
| 59 | 0x80 | FVSL | - (via fn) |
| 59 | 0x1f | FRR | 16.0x1f |

#### WT (`MA5_WT`) (voice byte 0 = 0x01, 18 bytes)

| byte | mask | field | from MA-7 voice byte.mask |
|---|---|---|---|
| 0 | 0xff | type flags (0x01) | byte 0 |
| 1 | 0xff | Fs(MSB) | 1.0xff |
| 2 | 0xff | Fs(LSB) | 2.0xff |
| 3 | 0xf8 | Panpot | 3.0xf8 |
| 3 | 0x07 | KSO | - (via fn) |
| 4 | 0xc3 | LFO, Mode | 4.0xc3 |
| 4 | 0x20 | PE | 3.0x01 <<5 |
| 4 | 0x08 | ALE | 0.0x02 <<2 |
| 5 | 0xfb | SR, XOF, SUS, KSR | 5.0xfb |
| 5 | 0x04 | PEGE | - (via fn) |
| 6 | 0xff | RR, DR | 6.0xff |
| 7 | 0xff | AR, SL | 7.0xff |
| 8 | 0xfc | TL | 8.0xfc |
| 8 | 0x03 | KSL | - (via fn) |
| 9 | 0x77 | DAM, EAM, DVB, EVB | 9.0x77 |
| 10 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 11 | 0xff | StartAddressOffset(MSB) | 10.0xff |
| 12 | 0xff | StartAddressOffset(LSB) | 11.0xff |
| 13 | 0x7f | LoopPoint(H) | 12.0x7f |
| 14 | 0xff | LoopPoint(L) | 13.0xff |
| 15 | 0x7f | EndPoint(H) | 14.0x7f |
| 16 | 0xff | EndPoint(L) | 15.0xff |
| 17 | 0xff | RM, WaveID | 16.0xff |

#### WT + AL (`MA5_WTAL`) (voice byte 0 = 0x03, 34 bytes)

| byte | mask | field | from MA-7 voice byte.mask |
|---|---|---|---|
| 0 | 0xff | type flags (0x03) | byte 0 |
| 1 | 0xff | Fs(MSB) | 28.0xff |
| 2 | 0xff | Fs(LSB) | 29.0xff |
| 3 | 0xf8 | Panpot | 30.0xf8 |
| 3 | 0x07 | KSO | - (via fn) |
| 4 | 0xc3 | LFO, Mode | 31.0xc3 |
| 4 | 0x20 | PE | 30.0x01 <<5 |
| 4 | 0x08 | ALE | 0.0x02 <<2 |
| 4 | 0x04 | NOISE | 1.0x02 <<1 |
| 5 | 0xfb | SR, XOF, SUS, KSR | 32.0xfb |
| 5 | 0x04 | PEGE | - (via fn) |
| 6 | 0xff | RR, DR | 33.0xff |
| 7 | 0xff | AR, SL | 34.0xff |
| 8 | 0xfc | TL | 35.0xfc |
| 8 | 0x03 | KSL | - (via fn) |
| 9 | 0x77 | DAM, EAM, DVB, EVB | 36.0x77 |
| 10 | 0x0f | EXAR, EXDR, EXSR, EXRR | - (via fn) |
| 11 | 0xff | StartAddressOffset(MSB) | 37.0xff |
| 12 | 0xff | StartAddressOffset(LSB) | 38.0xff |
| 13 | 0x7f | LoopPoint(H) | 39.0x7f |
| 14 | 0xff | LoopPoint(L) | 40.0xff |
| 15 | 0x7f | EndPoint(H) | 41.0x7f |
| 16 | 0xff | EndPoint(L) | 42.0xff |
| 17 | 0x1f | Q | 2.0x1f |
| 18 | 0xf0 | FLFO_DEPTH & MODE | 17.0x78 <<1 |
| 18 | 0x08 | FLFO_RST | 26.0x40 >>3 |
| 18 | 0x07 | FLFO_FREQ | 17.0x07 |
| 19 | 0x1f | FC0(H) | 3.0x3e >>1 |
| 20 | 0x80 | FC0(L)-1 | 3.0x01 <<7 |
| 20 | 0x7f | FC0(L)-2 | 4.0x7f |
| 21 | 0x1f | FC1(H) | 5.0x3e >>1 |
| 22 | 0x80 | FC1(L)-1 | 5.0x01 <<7 |
| 22 | 0x7f | FC1(L)-2 | 6.0x7f |
| 23 | 0x1f | FC2(H) | 7.0x3e >>1 |
| 24 | 0x80 | FC2(L)-1 | 7.0x01 <<7 |
| 24 | 0x7f | FC2(L)-2 | 8.0x7f |
| 25 | 0x1f | FC3(H) | 9.0x3e >>1 |
| 26 | 0x80 | FC3(L)-1 | 9.0x01 <<7 |
| 26 | 0x7f | FC3(L)-2 | 10.0x7f |
| 27 | 0x1f | FC4(H) | 11.0x3e >>1 |
| 28 | 0x80 | FC4(L)-1 | 11.0x01 <<7 |
| 28 | 0x7f | FC4(L)-2 | 12.0x7f |
| 29 | 0x80 | FXOF | - (via fn) |
| 29 | 0x1f | FAR | 13.0x1f |
| 30 | 0x80 | FSUS | - (via fn) |
| 30 | 0x1f | FDR | 14.0x1f |
| 31 | 0x80 | FKSL | 27.0x01 <<7 |
| 31 | 0x1f | FSR | 15.0x1f |
| 32 | 0x80 | FVSL | - (via fn) |
| 32 | 0x1f | FRR | 16.0x1f |
| 33 | 0xff | RM, WaveID | 43.0xff |

## TODO

- every level 0x02 message that occurs in `tmp/samples/n703id` or in the ~4400
  file `~/Public/np2/mfi` corpus is decoded now (`Function2_240_12 / _14 / _15`,
  `Function2_241_10 / _11 / _13 / _14 / _15`, `Function2_242_7`,
  `Function2_243_3 / _10 / _11`), but none of them can be applied to the MIDI
  synthesizer the sequencer plays with - they only decode and log. driving a real
  MA-7 voice would need an MA-7 tone generator (see the `SimpleSoundbank` idea in
  the adpcm sync notes)
- `02.f1.x7 / x8 / x9 / xc` are implemented from the converter alone (internal
  events 209 ~ 211 and 214 all push a payload length of 1), nothing in either
  corpus uses them so the decoding is untested against real data. `xc` has no
  level 0x01 counterpart and is unidentified - **TODO more investigation**
- level 0x01 is decoded too now (`Function1_240_4 / _5 / _6 / _8`,
  `Function1_241_7 / _8 / _9 / _10 / _11`, `Function1_242_7`,
  `Function1_243_1 / _7 / _10`). still missing: `01.f0._3` (FM basic waveform),
  `01.f0._7` (stream waveform control), `01.f3._2` (UserEvent) and `01.f3._5`
  (AL channel) - nothing in either corpus uses them
- **every** NEC machine dependent message in `~/Public/np2/mfi` is handled now:
  13039 of 13039, none unhandled, none failing to decode. Same for
  `tmp/samples/n703id`.
- **TODO more investigation** on the two that are handled without being
  understood:
  - `f2._6` (`Function242_6`), 3 messages in 3 MFi 2.0 files. Decoded as a
    device control next to vibrator and LED because that is what it looks like,
    but what it drives is unknown.
  - the level `0x81` messages (`Function129_0 / _2 / _3`, see the "Level 0x81"
    section). Reading them as level 0x01 decodes them perfectly, but why bit 7
    is set at all is unknown - one file in ~4400 does it.
- name `02.f0.04 / 05 / 08 / 0a / 0b`, `02.f1.xc`, `02.f3.01 / 02 / 07 / 09 / 0a`
  (reachable in the converter, unused by the `n703id` samples)
- identify the 9 trailing bytes of the `voice[0] = 0x05 / 0x07` tone variants
- outside this package: `255.b.233` turned out to be the fine half of the pitch
  bend and is implemented as `vavi.sound.mfi.vavi.track.PitchBendFineMessage`
  (136395 events in the corpus). Three plain track messages are still
  unidentified and stay `UndefinedMessage`, which handles them without losing
  anything:

  | key | count | what is known |
  |---|---|---|
  | `255.b.232` (`0xe8`) | 12643 | a per voice 6 bit control resting at 32, like the pitch bend pair, but only 1410 of them have a pitch bend at the same time on the same voice, so it is **not** part of it. The MA-5 converter neither reads nor writes it. |
  | `127.e.240` | 5098 | a 6 byte message of the audio (0x7f) class, MFi 4.0 / 5.0 only, e.g. `00 0a 8f 80 00 00` - the first byte counts 0, 1, 2 within a file |
  | `127.b.144` | 479 | one data byte, audio class, MFi 4.0 / 5.0 only |

  naming any of them needs a document none of the DLLs carry
- `tmp/samples/n703id/02 TRANSPARENT.mld` is a good ADPCM + tone sample,
  `14 Piano.mld` the only one with a key split voice, `18 Cyber Call.mld` the
  smallest complete one
