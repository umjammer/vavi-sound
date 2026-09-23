# vavi.sound.mfi.vavi.panasonic

Provides Panasonic-specific message processing classes.

A Panasonic machine dependent message is a `0xff 0xff` sysex whose payload starts with
the vendor byte `0x41` (`VENDOR_PANASONIC | CARRIER_DOCOMO`), the byte after it is the
function. `PanasonicSequencer` builds the lookup key from the function byte alone, so the
key is `64.<function>`. The sequencer and the 0x8# ADPCM functions are in
`vavi-sound-nda`; the package is split over the two artifacts, this one has the rest.

- 0x40 ... Panasonic vendor id

| no   | function                  | id     | class       | where          |
|------|---------------------------|--------|-------------|----------------|
| 0x10 | Wave Data                 | 64.16  | Function16  |                |
| 0x11 | Wave Voice Parameter      | 64.17  | Function17  |                |
| 0x12 | Wave Voice Setting        | 64.18  | Function18  |                |
| 0x81 | Wave Channel Volume       | 64.129 | Function129 | vavi-sound-nda |
| 0x83 | Wave Packet Data          | 64.131 | Function131 | vavi-sound-nda |
| 0x86 | Wave Packet Data 3        | 64.134 | Function134 | vavi-sound-nda |
| 0x90 | Wave Data (MFi 4.0)       | 64.144 | Function144 |                |
| 0x91 | Wave Parameter            | 64.145 | Function145 |                |
| 0x92 | Voice Parameter           | 64.146 | Function146 |                |
| 0x93 | Voice Parameter 2         | 64.147 | Function147 |                |
| 0x94 | voice block start         | 64.148 | Function148 |                |
| 0xa0 | sound source wide setting | 64.160 | Function160 |                |
| 0xb0 | sound source parameter 0  | 64.176 | Function176 |                |
| 0xb1 | sound source parameter 1  | 64.177 | Function177 |                |
| 0xf0 | ? (unidentified)          | 64.240 | Function240 |                |

Read from the ~4400 file corpus at `~/Public/np2/mfi` (326 files with a Panasonic
message), no document being available.

## 0x90 ~ 0xb1, the MFi 4.0 plug in

`幸せをありがとう.mld` of the mission and the rest of the files the Panasonic plug ins
wrote (`supt` `MFi4PlugIn_P`, `P_PlugIn`, `P_Plugin`) carry the wave table voice
block of the Fujitsu plug in byte for byte under their own vendor byte, so these are
the `fujitsu` classes with the vendor byte overridden; see that package for the layouts.
Its checks hold for Panasonic's 219 files as well: each 0x91 starts at the 0x90 of its
voice and loops inside it, 658 of 658, each 0x92 record carries its voice number, 661 of
661, and the 0x93 / 0xb0 / 0xb1 width byte rule holds for all 1760.

The exceptions are files, not layouts: two have a 0x90 shorter than its length field
says (`かざぐるま.mld`, `Introduction to the second session.mld`), and one lacks its
first wave, the 0x90 of voice 1 landing at 704 where the 0x91 says it does.

0x94 is Panasonic's own: `94 00`, 153 of them in 27 files, each right before the 0x90
that starts a voice block, one per block. What its byte would say other than 0 is not
known.

## 0x10 ~ 0x12

The wave table voice messages Sharp writes (see the `sharp` package), in two files.

## TODO

- 0xf0 (`f0 80`, two files of `Ringtones from Cami P901iS`).
- as for Fujitsu, nothing of the wave table reaches a synthesizer yet.
