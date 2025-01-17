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
                [MasterTrackChunk]                  (MSTR)
                crc                                                     *

```

## References

 * https://github.com/but80/smaf825/
 * https://github.com/but80/fmfm.core
 * https://github.com/but80/go-smaf
 * https://lpcwiki.miraheze.org/wiki/Yamaha_SMAF
 * https://archive.org/details/yamaha_ymu762_datasheet
 * https://gist.github.com/bryc/e85315f758ff3eced19d2d4fdeef01c5/
 * https://github.com/denjhang/MA-3-MegaMod

## TODO

 * https://github.com/umjammer/vavi-sound/discussions/21
