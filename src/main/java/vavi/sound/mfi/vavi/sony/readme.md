# vavi.sound.mfi.vavi.sony

Provides Sony-specific message processing classes.

A Sony machine dependent message is a `0xff 0xff` sysex whose payload starts with the
vendor byte `0x31` (`VENDOR_SONY | CARRIER_DOCOMO`), the byte after it is the function:

```
 ff ff <len:2> 31 <function> <payload...>
```

`SonySequencer` builds the lookup key from the function byte alone, so the key is
`48.<function>`. The sequencer and the 0x8# ADPCM functions are in `vavi-sound-nda`; the
package is split over the two artifacts, this one has the rest.

- 0x30 ... Sony vendor id

| no          | function                     | id          | class                 | where          |
|-------------|------------------------------|-------------|-----------------------|----------------|
| 0x10        | MA-3 / MA-5 extension        | 48.16       | Function16            |                |
| 0x30        | Wave Data                    | 48.48       | Function48            |                |
| 0x31        | Wave Voice Parameter         | 48.49       | Function49            |                |
| 0x32        | Wave Voice Setting           | 48.50       | Function50            |                |
| 0x81        | Wave Channel Volume          | 48.129      | Function129           | vavi-sound-nda |
| 0x82        | Wave Channel Panpot          | 48.130      | Function130           | vavi-sound-nda |
| 0x83        | Wave Packet Data             | 48.131      | Function131           | vavi-sound-nda |
| 0x84        | Wave Packet Data 3           | 48.132      | Function132           | vavi-sound-nda |
| 0x8f        | Wave Setup                   | 48.143      | Function143           | vavi-sound-nda |
| 0xb0        | sound source parameter 0     | 48.176      | Function176           |                |
| 0xb1        | sound source parameter 1     | 48.177      | Function177           |                |
| 0xe0 ~ 0xe3 | Pitch Bend LSB, voice 0 ~ 3  | 48.224 ~ 227 | Function224 ~ 227    |                |
| 0xe8 ~ 0xeb | Pitch Bend MSB, voice 0 ~ 3  | 48.232 ~ 235 | Function232 ~ 235    |                |
| 0xef        | Pitch Bend Range             | 48.239      | Function239           |                |

No document was available, so all of these were read from the ~4400 file corpus at
`~/Public/np2/mfi` (3.2k files with a Sony message). What made it possible is the
`upload_melody*` directories: they carry every song once per maker (`_d40`, `_f16`,
`_n40`, `_p16`, `_p40`, `_sh40`, `_so16`, `_so40`), so a Sony message can be lined up
with what the NEC file of the same song writes at the same place, and NEC's messages
are known from its converter DLLs (see the `nec` package).

## 0xe0 ~ 0xeb, the pitch bend of the MFi 2.0 writer (`_so16`)

A 14 bit pitch bend in two messages, the MIDI way, the low 2 bits of the function byte
being the voice: `e0 + voice <lsb>` then `e8 + voice <msb>`, `0x2000` (msb `0x40`) at
rest. The LSB is cached and the MSB commits - every one of the 107430 LSB messages of
the corpus is followed by the MSB of the same voice at the same delta. `e8 + voice` also
comes in a two byte form, `<lsb> <msb>`, that stands alone (7725 messages).

Over `0_A645_C194_01_NULL_01_so16.mld` (the file of the mission) the 122 values are,
point by point, the pitch bend of its `_n40` twin within the 3 bits the NEC one loses
(`1 4097 6001 8192` against `0 4096 6000 8192`).

`0xef <voice:6-5> <range:4-0>` is the pitch bend range the same writer puts at the head
of a track in place of the MFi 0xe7 it never writes: 2 in 11147 of 12445, 12 in 1274,
and equal to the `_n40` twin's 0xe7 in all 433 voices both have.

## 0x10, the MA-3 / MA-5 extension (`_so40`)

What NEC writes as its level 0x01 messages, one function with a sub function byte
(`<channel:7-6> <sub:5-0>`) instead of NEC's group / function bytes. The payloads are
NEC's (the stream wave of a `_so40` file is the one of its `_n40` twin, header and
adpcm):

| sub  | payload      | NEC   | meaning                                              |
|------|--------------|-------|------------------------------------------------------|
| 0x01 | 1            |       | ? head of track 0, 0 in 3149 of 3180                 |
| 0x02 | 1            |       | ? head of track 0, 1 in 2925 of 2928                 |
| 0x04 | 2 + 31 (17)  | f0.04 | FM tone: bank, program, 4 (2) operator voice         |
| 0x05 | 2 + 16       | f0.05 | WT tone: bank, program, VM35 PCM voice               |
| 0x06 | 2 + n        | f0.06 | WT wave: wave id, format (0: 4 bit adpcm), wave      |
| 0x07 | 4 + n        | f0.07 | stream wave: stream, format, rate:2, adpcm           |
| 0x08 | 16           | f2.07 | channel status                                       |
| 0x09 | 2            | f1.x3 | StreamOn: stream, velocity                           |
| 0x0a | 1            | f1.x5 | StreamOff: stream                                    |
| 0x0c | 1            | f1.x7 | Hold1, per channel                                   |
| 0x11 | 1            | f3.01?| FM mode setting, agrees with NEC's in 1343 of 1485   |

The tone records address a voice the way NEC's do: the bank is always one the song
selects, the program either one it selects (a melody voice, 536 of 536) or, bit 7 of
the bank set, a note key it plays on that bank (a drum voice, 535 of 535). A WT tone
that plays a wave of its own names a wave a 0x06 of the same file sends, 119 of 119.

Like NEC's, the tones and waves are handed to the synthesizer as SMAF exclusives
(`YamahaMfiExclusive`) and the stream is played by a `YamahaAudioEngine`.

## 0x30 ~ 0x32, 0xb0, 0xb1

The wave table voice messages Sharp writes as 0x10 ~ 0x12 (see the `sharp` package),
moved up by 0x20, and the 0xb0 / 0xb1 parameters every MFi 4.0 plug in writes (see the
`fujitsu` package). Only 16 / 17 files have them.

## Checking it

`SonyFunctionTest.corpus` (needs `-Dvavi.test=ide` and `mfi.dir` in `local.properties`)
runs every Sony message of the corpus through its function and checks the layouts
against the song: that each LSB is committed by an MSB of its voice, that each tone
names a bank and a program the song selects, that each WT tone's own wave and each
StreamOn's stream was sent, and that each wave table voice has its wave.

## TODO

- the pitch bends and Hold1 are decoded and logged only. A machine dependent message
  reaches its function with no track, so the MIDI channel a bend belongs to is not known
  there; making them heard needs `MachineDependentMessage#getMidiEvents` to convert them
  instead.
- 0x10 subs 0x01 and 0x02, and whether 0x11 really is the FM mode.
