# vavi.sound.smaf.chunk

SMAF サウンド関連のファイル構造を表すクラスを提供します．

## Abstract

SMAF ファイルの読み込み時に使用されます。

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
