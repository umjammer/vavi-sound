# vavi.sound.dxm

DXM (Feelsound, PHS, `MCDF`) reader.

The sequence part is reverse engineered from `PsmPlay.exe` (PsmPlayer 5.0, APO), function at `0x41655e`.
It scans the file for `CThd`/`CTrk` and reads them with its SMF reader,
`MCDF` itself is skipped. The index is inferred from the corpus.

## file

```
"MCDF"
index ... u16 id, u32 offset (from the top of the file), u32 size
 :
u16 0xffff
entry data ...
```

### index

The upper byte of the id seems to be a part, 0x00## is for the whole content, 0x02## is for the sequence.
A file lists ids with size 0 too.

| id     | data                                                        | |
|--------|-------------------------------------------------------------|-|
| 0x0000 | "01.0"                                                      | format version |
| 0x0081 | u32                                                         | file size |
| 0x0082 | "2856"                                                      | ? |
| 0x0083 | u16 year, u8 month, day, hour, minute, second, ?            | date |
| 0x0084 | "01.0"                                                      | version |
| 0x00c0 | shift_jis                                                   | title |
| 0x0240 | `CThd` + `CTrk`s                                            | sequence (PsmPlay) |
| 0x0280 | u32                                                         | play time [msec] |
| 0x0281 | u32                                                         | size of 0x0240 |
| 0x0282 | "2856"                                                      | ? |
| 0x0283 | same as 0x0083                                              | date |
| 0x0284 | "01.0"                                                      | version |
| 0x02c0 | shift_jis                                                   | title |
| 0x02c4 | shift_jis, "PS-PLAYER V7.10"                                | authoring tool |

others (0x0001, 0x0010, 0x0011, 0x0020, 0x0021, 0x0030, 0x0031, 0x008#, 0x00c#, 0x020#, 0x0285, 0x0286, 0x02c#) are unknown.

## sequence

```
"CThd" u32 6, u16 format, u16 tracks, u16 division (24)
"CTrk" u32 length, events ...
```

`CTrk` is SMF `MTrk` except

| status | SMF                | DXM          |
|--------|--------------------|--------------|
| 0x8#   | note off key vel   | **key only** |
| 0xe#   | pitch bend lsb msb | **msb only** |

## verified

 * 1044 of 1047 `MCDF` files in `~/Public/np2/feel sound box (dxm)` parse to end of track at the end of `CTrk`,
   the others are truncated files

## TODO

 * converting to MIDI
 * ADPCM, OKI synthesizer parts (not in the corpus)
