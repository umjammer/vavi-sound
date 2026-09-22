# vavi.sound.mfi.vavi.sharp

Provides Sharp-specific message processing classes.
   
- 0x70 ... Sharp vendor id

| no   | function                    | id      | class       |
|------|-----------------------------|---------|-------------|
| 0x10 | Wave Data                   | 112.16  | Function16  |
| 0x11 | Wave Voice Parameter        | 112.17  | Function17  |
| 0x12 | Wave Voice Setting          | 112.18  | Function18  |
| 0x40 | ? (unidentified)            | 112.64  | Function64  |
| 0x81 | Wave Channel Volume setting | 112.129 | Function129 |
| 0x82 | Wave Channel Panpot setting | 112.130 | Function130 |
| 0x83 | Wave Packet Data            | 112.131 | Function131 |
| 0x84 | Wave Packet Data3           | 112.132 | Function132 |
| 0x8f | Wave Setup                  | 112.143 | Function143 |
| 0x90 | Wave Data (MFi 4.0)         | 112.144 | Function144 |
| 0x91 | Wave Parameter              | 112.145 | Function145 |
| 0x92 | Voice Parameter             | 112.146 | Function146 |
| 0x93 | Voice Parameter 2           | 112.147 | Function147 |
| 0xa0 | sound source wide setting   | 112.160 | Function160 |
| 0xb0 | sound source parameter 0    | 112.176 | Function176 |
| 0xb1 | sound source parameter 1    | 112.177 | Function177 |

The 0x8# functions are the ADPCM stream (`FuetrekAudioEngine`). The rest were read from
the ~4400 file corpus at `~/Public/np2/mfi` (about 1400 files with a Sharp message), no
document being available.

## 0x90 ~ 0xb1, the MFi 4.0 plug in

`DSどうぶつ_たぬきﾃﾞﾊﾟｰﾄ.mld`, the file of the mission, is written by
`MFi4PlugIn_SH 01.00.06` (its `supt` sub chunk), the Sharp sibling of the
`MFi4PlugIn_F 01.00.06` the Fujitsu F901iC ringtones come from, and the two write the
very same 0x9# ~ 0xb# messages - `b0 01 00 03 01 00` and `b1 01 01 02 00 01` byte for
byte - only the vendor byte differs. So these classes are the `fujitsu` ones with the
vendor byte overridden (`FujitsuFunction#getVendor()`); see the `fujitsu` package for
the layouts. The corpus has 774 0xb0, 749 0xb1 and one or two files of the others, and
`SharpFunctionTest.corpus` checks the 0x90 / 0x92 ones the way Fujitsu's are checked.

## 0x10 ~ 0x12, the wave table voice

A voice of its own sample, registered with three messages numbered by the voice (1553
voices in the corpus, 1477 Sharp's, the rest Sony's, who writes the same messages as
0x30 ~ 0x32):

```
 10 <n> 01 <length:3> <loop start:3> <loop end:3>    the wave, header
 10 <n> 02 <length:2> <samples, signed 8 bit ...>    the wave, samples
 11 <n> 02 2c <44 byte record>                       the voice parameters
 12 <n> 00 04 80 <drum> <bank> <program | note key>  what it plays for
 12 <n> 10 01 80                                     ? (149 of them)
```

- each header is followed by the samples of its wave and both lengths are the length of
  the samples, 1553 of 1553; `loop end` is `length - 3` in 1550.
- each 0x11 has the wave of its number and the record repeats the number at +1, 1553 of
  1553. The rest of the record is not settled.
- the 0x12 bank is one the song selects (`ChangeBank`); a melody voice's program is
  one it selects with the bank (`ChangeVoice`), 1220 of 1222, and a drum voice's last
  byte is a note key the song plays on that bank, 870 of 870 - the NEC tone addressing.

## TODO

- the 0x11 record, the 0x12 parameter 0x10 and 0x40.
- nothing of 0x10 ~ 0x12 is handed to a synthesizer yet; the voice parameters would
  have to be understood to make a soundbank of them.
