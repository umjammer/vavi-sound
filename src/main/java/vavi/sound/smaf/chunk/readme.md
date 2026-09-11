# vavi.sound.smaf.chunk

Provides a class that represents the file structure related to SMAF sound.

### Abstract

Used when reading SMAF files.

### Structure

```
                                                                        wav2mld

        FileChunk                                   (MMMD)              *
                ContentsInfoChunk                   (CNTI)              *
                    SubData*
                [OptionalDataChunk]                 (OPDA)
                    DataChunk*                      (Dch*)
                    ???Chunk                        (Pro*)
                ScoreTrackChunk*                    (MTR*)
                    [SeekAndPhraseInfoChunk]        (MspI)
                    SequenceDataChunk               (Mtsq)
                        SmafMessage*
                    ???                             (Mthv)
                        ???                         (Mhvs)
                PcmAudioTrackChunk*                 (ATR*)              *
                    [SeekAndPhraseInfoChunk]        (AspI)              *
                    [SetupDataChunk]                (Atsu)
                    AudioSequenceDataChunk          (Atsq)              *
                        SmafMessage*
                    WaveDataChunk*                  (Awa*)              *
                GraphicsTrackChunk*                 (GTR*)
                MMMGChunk*                          (MMMG)
                    VoiceChunk                      (VOIC)
                        EXWVChunk*                  (EXWV)
                        ExclusiveVoiceChunk*        (EXVO)
                    SequenceDataChunk*              (SEQU)
                        SmafMessage*
                [MasterTrackChunk]                  (MSTR)
                crc                                                     *

```

### "MMMG" phrase track

A phrase (ring tone) track. The 2 bytes before the first sub chunk are
`<voice format> 0x14`, the voice format being `1` for VMA (MA-1/MA-2) and `2` for
VM35 (MA-3/MA-5) voices, which decides how "EXVO" is read.

"VOIC" carries the voices as machine dependent exclusives. For the VM35 format a
wavetable voice is a pair:

 * "EXWV" ... the wave (4 bit ADPCM) itself, keyed by a wave id
 * "EXVO" ... the VM35 PCM voice which links to that wave id and holds the sampling
   rate, the loop/end point and the envelope

"EXWV" always comes before the "EXVO" that refers to it.

```
 "EXWV"
  +0        0xff            exclusive prefix
  +1        0xf1            "long" exclusive (binary 8 bit data, 16 bit length)
  +2 ~ +3   length          little endian, counts +4 ... 0xf7 inclusive
  +4        0x43            manufacturer id (YAMAHA)
  +5        0x05            SMAF phrase (MA-3/MA-5 class) exclusive
  +6        0x00            sub id: wave data
  +7        wave id         0 ~ 15
  +8 ~      wave data       length - 5 bytes, 4 bit ADPCM, written verbatim into wave ram
  +length+3 0xf7            end of exclusive

 "EXVO" (VM35 PCM voice, length 0x16)
  +0        0xff
  +1        0xf0            plain (7 bit safe) exclusive, 8 bit length
  +2        length          0x16
  +3        0x43            manufacturer id (YAMAHA)
  +4        0x05
  +5        0x02            voice type: VM35 voice
  +6        bank            0x01 / 0x81
  +7        program
  +8 ~ +23  VM35 PCM voice  16 bytes, +23 is "RM|WaveID" (RM = 0: the "EXWV" wave,
                            RM = 1: a preset wave 0 ~ 6)
  +24       0xf7
```

Note that "EXWV" is *not* 7 bit safe (that is why it uses `0xf1` and a 16 bit length,
where the MA-5 bulk wave exclusive `43 79 06 7F 03` packs 7 bytes into 8 instead), and
that the wave data has no header of its own: it is the raw wave ram image. The number of
samples is `(end point + 1)`, that is `wave data bytes * 2`, which is what says the data
is 4 bit.

Reversed from the aarch64 `libM7_EmuSmw7.so` of MMF-Player
(`YAMAHA::MaPhrCnv_ReqVoice`, `YAMAHA::MaSndDrv_SetWtWave`,
`YAMAHA::MaDevDrv_SendDirectRamData`).

## References

 * https://github.com/but80/smaf825/
 * https://github.com/but80/fmfm.core
 * https://github.com/but80/go-smaf
 * https://lpcwiki.miraheze.org/wiki/Yamaha_SMAF
 * https://archive.org/details/yamaha_ymu762_datasheet
 * https://gist.github.com/bryc/e85315f758ff3eced19d2d4fdeef01c5/
 * https://github.com/denjhang/MA-3-MegaMod
 * https://github.com/but80/smaf825/blob/v1/smaf/voice/vm35_pcm_voice.go

## TODO

 * ~~https://github.com/umjammer/vavi-sound/discussions/21~~
