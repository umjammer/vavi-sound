# vavi.sound.mfi.vavi.fujitsu

Provides Fujitsu-specific message processing classes.

A Fujitsu machine dependent message is a `0xff 0xff` sysex whose payload starts with the
vendor byte `0x21` (`VENDOR_FUJITSU | CARRIER_DOCOMO`), the byte after it is the function:

```
 ff ff <len:2> 21 <function> <payload...>
                     |
                     +-- 0x01 .. 0xb1
```

`FujitsuSequencer` builds the lookup key from the function byte alone, so the key is
`32.<function>`.

- 0x20 ... Fujitsu vendor id

| no   | function                  | id     | class       | where                          |
|------|---------------------------|--------|-------------|--------------------------------|
| 0x01 | sound source setup        | 32.1   | Function1   |                                |
| 0x02 | ? (unidentified)          | 32.2   | Function2   |                                |
| 0x81 | Wave Channel Volume       | 32.129 | Function129 | vavi-sound-nda                 |
| 0x82 | Wave Channel Panpot       | 32.130 | Function130 |                                |
| 0x83 | Wave Packet Data 2        | 32.131 | Function131 | vavi-sound-nda                 |
| 0x86 | Wave Packet Data 3        | 32.134 | Function134 | vavi-sound-nda                 |
| 0x90 | Wave Data                 | 32.144 | Function144 |                                |
| 0x91 | Wave Parameter            | 32.145 | Function145 |                                |
| 0x92 | Voice Parameter           | 32.146 | Function146 |                                |
| 0x93 | Voice Parameter 2         | 32.147 | Function147 |                                |
| 0xa0 | sound source wide setting | 32.160 | Function160 |                                |
| 0xa1 | per voice setting         | 32.161 | Function161 |                                |
| 0xb0 | sound source parameter 0  | 32.176 | Function176 |                                |
| 0xb1 | sound source parameter 1  | 32.177 | Function177 |                                |

The 0x8# functions are the MFi 2.0 / 3.0 ADPCM stream, which goes to the ROHM audio
engine (`vavi.sound.mobile.RohmAudioEngine`). The 0x9# ~ 0xb# ones are what the
`MFi4PlugIn_F` writer puts in an MFi 4.0 file (`supt` = `MFi4PlugIn_F 01.00.06`, e.g.
the F901iC ringtones), a wave table of its own next to that stream. 0x01 is the MFi 2.0
sound source setup and carries its own sub function byte. All of them but the three
`vavi-sound-nda` ones are what this package adds.

The 0x9# ~ 0xb# messages are not Fujitsu's own: the Sharp plug in
(`MFi4PlugIn_SH 01.00.06`) and the Panasonic ones (`MFi4PlugIn_P`, `P_PlugIn`) write
them byte for byte, and Sony writes 0xb0 / 0xb1, under their own vendor byte.
`FujitsuFunction#getVendor()` is what the `sharp`, `panasonic` and `sony` packages
override to reuse these classes.

## 0x01, the MFi 2.0 sound source setup

The only Fujitsu function with a sub function byte after the function byte:

```
 ff ff <len:2> 21 01 <sub> <payload...>
```

| sub  | payload | count | meaning                                           |
|------|---------|-------|---------------------------------------------------|
| 0x01 | 16      | 8     | a voice, its index field holding the program      |
| 0x02 | 16      | 59    | a voice, its index field counting up              |
| 0x03 | 9751    | 1     | a wave, 4 bit adpcm by the look of it             |
| 0x05 | 2       | 4     | ? - `01 ff` three times then `01 28`              |
| 0x06 | 1       | 1     | ? - `3f`, the top of a 6 bit range, so a volume   |

Only the voice form is settled, and it is settled well. Over the 67 voices of the
corpus:

```
 <key number> <index> <bank|drum> <program> <?> 0x01 <op1:5> <op2:5>
```

- `bank` is a bank the file really selects with `ChangeBank` (`0xe1`), 67 of 67, and
  its bit 7 is the drum flag - the same way the NEC tone messages are built.
- `program` of a melody voice is a program the file really selects with `ChangeVoice`
  (`0xe0`), 43 of the 45 melody voices; the two left over are one file that registers
  a voice it never plays. A drum voice's is not, it is the index inside the kit, again
  as in the NEC messages where the key number rides along beside it.
- `key number` is non zero exactly when the drum flag is set, 67 of 67. The grown corpus
  (51834 voices, 4879 files) agrees but for 13 drum voices with key 0, all of them empty
  slots (`01 00 00 00 00 a0 00 00 00 00 a0`, drum programs 0x32 ~ 0x34) that the
  `Yamaha S2M_0100` converter writes - `Function1#isEmpty()`, which the check skips.
- `index` counts 0, 1, 2 ... over the messages of a file for sub 0x02 (59 of 59); for
  sub 0x01 it is the program instead (8 of 8). That is the only difference between the
  two forms - both are MFi 0200 files, so it is not a version thing.

The 12 voice bytes are one byte whose low 3 bits are 0 in 66 of the 67 (a panpot in
bits 7-3 would look like that, as the MA-5 voice's `Panpot, BO` byte does), then 0x01
in all 67, then two 5 byte operators - a 2 operator FM voice. Which field of an
operator is which is not settled.

## How the 0x9# ~ 0xb# functions were read

No MFi document for them was available, so they were guessed from the ~4400 file corpus
at `~/Public/np2/mfi`: 56 of its files write a Fujitsu message at all, 30 of them a wave
table voice block (48 blocks in all) and 45 the 0xb0 / 0xb1 pair. Only the counts below
back the field layouts up; where the corpus does not settle a meaning the class hands
the bytes out as they are and says so.

### The wave table voice block

A file registers 1 ~ 4 wave table voices, each as four messages in this order:

```
 90 <dst offset:2> <length:2> <wave ...>       the samples
 91 <0x0a * n> <0x0a> <10 byte record>          how to play them
 92 <0x20 * n> <0x20> <32 byte record>          the voice
 93 <n> <width> <parameter> <value:2>           one more voice parameter
```

then `b0` and `b1` once for the whole file. `a0` follows before the first
`ChangeBank` (`0xe1`) of the song, and `a1` - the one file that has it - after the
`ChangeBank` / `ChangeVoice` pair of each voice.

### 0x90 Wave Data

`data[9..10]` is `data.length - 11` in all 48 messages, so it is the length of the wave
and `data[7..8]` is the other 16 bit field. Within a file the messages come with
`data[7..8]` ascending, the first 0 and each next one exactly the previous offset plus
the previous length rounded up to a multiple of 4 (48 of 48) - a destination offset into
wave memory, waves packed 4 byte aligned.

The samples are signed 8 bit: read that way the mean of every wave lands within +-4 of
zero and the mean absolute step between neighbours is 2.8 ~ 35, read unsigned the step
goes up to 135, i.e. unsigned wraps and signed does not.

### 0x91 Wave Parameter

```
 +0-1  0x8000 | start address of the wave, in 4 byte words
 +2-3  loop start [byte, from the start of the wave]
 +4-5  loop end
 +6-7  pitch, low 16 bits
 +8-9  pitch, high 16 bits
```

- the start address is `0x8000 | (waveOffset >> 2)` of the 0x90 that came right
  before in all 48 messages, `waveOffset` being its destination offset. The top bit
  is set in every one of them, presumably wave memory against a rom wave.
- `0 <= loopStart <= loopEnd < waveLength` in all 48, and `loopEnd` is within a few
  bytes of the end of the wave in most of them - a one shot attack with a short
  sustain loop at the end.
- the last two fields are one 32 bit number, low word first: the corpus has 17
  distinct pairs and no high word ever appears with two different low words nor the
  other way round. Read as `value / 0x01000000` it is exactly 1.0 for the one wave
  that is a flat single cycle and 0.61 ~ 3.39 for the rest, and 8 of the 17 values
  are an exact equal tempered semitone ratio (-4, -3, 0, +1, +2, +5, +7, +9) - a
  playback pitch factor.

### 0x92 Voice Parameter

Same `offset, length, record` shape as 0x91 with a 32 byte record, `0x20 * n`. It is
**not** a SMAF/MA VM35 voice (16 bytes); Fujitsu does not use a Yamaha sound source
here.

```
 + 0        always 0xff
 + 1        0xc0 | voice number, the same number the record offset gives (48 of 48)
 + 2        0x3c in 45 of 48, else 0x33 / 0x3f / 0x40 - a key number, 0x3c being
            middle C, so the key the 0x91 pitch belongs to
 + 3 ~ +12  0 but for a few files, +10 and +12 always 0
 +13 ~ +16  0x80 0x80 0x80 0x80 by default
 +17 ~ +20  0x7f 0x00 0x00 0x00 by default
 +21 ~ +24  0x80 0x80 0x80 0x80 by default
 +25 ~ +28  0x00 0x00 0x14 0x80 by default
 +29 ~ +31  0x7f 0x7f 0x00 by default
```

The two `0x80 0x80 0x80 0x80` runs each followed by four bytes look like two envelopes
(four rates, four levels), 0x80 being the neutral of a signed byte, but nothing in the
corpus settles which is which.

### 0x93, 0xb0, 0xb1 - the parameter messages

All three are exactly 7 bytes and share one payload (`ParameterFunction`):

```
 <target> <width> <parameter> <value:2>
```

`width` is what the corpus really settles: of the 231 messages, byte 11 is 0 in every
one of the 88 whose `width` is 0, and byte 10 is 0 in every one of the 143 whose
`width` is 1 - never an exception either way. Reading it as a width (0: the one byte at
10, 1: the 16 bit at 10-11) makes the values come out in useful ranges, 1 ~ 63 for
width 0 and 0 ~ 7 for width 1, where a plain 16 bit read leaves one of the two groups
a multiple of 256.

`target` is the voice number for 0x93 (the files that register 3 voices write 0x93 with
target 0, 1 and 2, the one that registers 4 writes 2 and 3) and always 1 for 0xb0 and
0xb1, which are written with or without a voice block and so belong to no voice.
`parameter` is 2, 3 or 4. Which parameter each number is, is not settled.

## Checking it

`FujitsuFunctionTest.corpus` (needs `-Dvavi.test=ide` and `mfi.dir` in
`local.properties`) runs every Fujitsu message of the corpus through its function and
checks the layouts against each other - that each 0x90 lands where the previous one
ended, that each 0x91 points at the 0x90 of its own voice and loops inside it, and that
each 0x92 record carries its own voice number, and that each 0x01 voice names a bank
the song selects and carries a key number exactly when it is a drum. 56 files, 606
messages, 48 waves, 48 wave table voices and 67 fm voices pass, and nothing falls
through to `UndefinedFunction` any more.

## TODO

- 0x02 is in the package only so that nothing Fujitsu reaches `UndefinedFunction`. The
  one file of the corpus that writes it has no `exst` sub chunk, so `VaviMfiFileFormat`
  cannot read it at all; scanning its track by hand turns up two messages,
  `21 02 62 00` and `21 02 02 00`, next to the 0x01 voices of the same file. Two bytes,
  the second 0 in both - not enough to name anything.
- the 0x01 sub functions 0x03, 0x05 and 0x06, one file and 6 messages between them.
- what the 0x92 record's fields are, and which parameter the numbers of 0x93 / 0xb0 /
  0xb1 select.
- nothing is handed to a synthesizer yet - the wave table is the sound source's own,
  not the ADPCM stream `vavi.sound.mobile.AudioEngine` plays, so it would need a
  soundbank to be heard.

## License

 * includes iMode@ information ... NTT DoCoMo (NDA)
