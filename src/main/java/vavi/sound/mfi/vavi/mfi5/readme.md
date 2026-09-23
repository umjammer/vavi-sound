# vavi.sound.mfi.vavi.mfi5

Provides the message processing classes of the carrier wide MFi 5 writer.

- 0x00 ... no vendor, `0x01` is `0x00 | CARRIER_DOCOMO`

"MFi 5" is not the name of a published spec, DoCoMo has none of that name: it is what
the files say of themselves, `vers` 0500 and `supt` `MFi5PlugIn_DoCoMo` (01.00.01,
01.00.02, 00.08.00). There are 311 files of it in the ~50000 file corpus at
`~/Public/np2/mfi`, and the 191 of them that have a machine dependent message have them
all under the vendor byte `0x01` and no other. `vavi-apps-mfiplayer`'s `MfiChip` takes
`0x01` of a `vers` 5 file for FueTrek PCM128.

| no   | function                 | id    | class       | the same as                         |
|------|--------------------------|-------|-------------|-------------------------------------|
| 0x10 | Wave Data                | 0.16  | Function16  | `sharp.Function16`                  |
| 0x11 | Wave Voice Parameter     | 0.17  | Function17  | `sharp.Function17`, more parameters |
| 0x12 | Wave Voice Setting       | 0.18  | Function18  | `sharp.Function18`                  |
| 0x40 | ? a channel parameter    | 0.64  | Function64  | `sharp.Function64`                  |
| 0xb0 | sound source parameter 0 | 0.176 | Function176 | `fujitsu.Function176`               |
| 0xb1 | sound source parameter 1 | 0.177 | Function177 | `fujitsu.Function177`               |

Read from the corpus, no document being available.

## 0x10 ~ 0x12, the wave table voice

The Sharp messages (see the `sharp` package) under the vendor byte `0x01`, 446 waves.
0x11 has more parameters than Sharp's 0x02, the byte after the voice number is a
parameter number and the next one the length of its value:

```
 11 <n> 02 2c <44 byte record>              the voice of a wave, as Sharp's
 11 <n> 10 06 <flags> 00 00 <bank> <program> ?   record +0 ~ +5, a voice of a built in tone, no wave
 11 <n> 20 01 <n + 1>                        record +8, link: the voice of oscillator B
 11 <n> 21 01 <balance>                      record +9, oscillator balance
 11 <n> 40 07 <3 words, 1 byte>              record +12 ~ +18, env A, changed while playing
 11 <n> 52 02 <word>                         record +27 ~ +28, shape w4
```

The record is the voice edit parameters of the fuetrek sound source, whose layout
`vavi.sound.fuetrek.FuetrekVoice.Template` of `vavi-apps-mfiplayer` has.

- a pair: two voices, the oscillators A and B of one. The second one has bit 1 of its
  flags (record +0 or the 0x10 value +0) set, and no 0x12 of its own, 53 of 53; the
  others have one but for 18 of 415. 0x20 names `n + 1`, 11 of 11, and a record whose +9
  (the balance) is not 0 has +8 = `n + 1`, 30 of 30.
- a built in tone voice has no wave, and its bank / program is the one the 0x12 of the
  pair says, 22 of 22.

## 0x40

The 11 bytes Sharp writes as 0x40, 120 of them. The low two bits of the first byte are a
channel of the track (tracks other than 0 have them too), the top two bits 2 or 1.

## 0xb0, 0xb1

The Fujitsu MFi 4.0 plug in messages, the width byte rule holds.

## the synthesizer

`vavi-apps-mfiplayer` plays them on its fuetrek sound source (`vavi.sound.mfi.fuetrek.UcsFunction`,
of a higher priority than these classes, which only read): the waves, the voices of a
preset tone, a pair as the two oscillators of a voice and the drum voices. See its
`vavi.sound.mfi.fuetrek` readme.

## TODO

- the last byte of 0x10 (record +5).
- 0x40.
