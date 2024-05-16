# vavi.sound.smaf.chunk

Provides a class that represents the file structure related to SMAF sound.

## Abstract

Used when reading SMAF files.

## Structure

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

## TODO
