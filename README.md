[![Release](https://jitpack.io/v/umjammer/vavi-sound.svg)](https://jitpack.io/#umjammer/vavi-sound)

# vavi-sound

| **SPI** |  **Codec** |  **Description** | **IN Status** | **OUT Status** | **SPI Status** | **Comment** |
|:--------|:-----------|:-----------------|:--------------|:---------------|:---------------|:------------|
| midi | MFi | Japanese cell phone format |  c | o | o | spi write only |
| midi | SMAF | YAMAHA cell phone format | c | o | o | spi write only |
| sampled | MFi | Japanese cell phone format |  x | o | x | |
| sampled | SMAF | YAMAHA cell phone format | o | o | o | |
| sampled | CCITT ADPCM | G711, G721, G723 | o | o | x | |
| - | DVI ADPCM | DVI ADPCM | o  | o | - | |
| - | IMA ADPCM | IMA ADPCM  | o | o | - | |
| - | MA ADPCM | YAMAHA ADPCM  | o  | o | - | |
| - | MS ADPCM | Microsoft ADPCM  | o  | o | - | |
| - | OKI ADPCM | OKI ADPCM  | o | o | - | |
| - | ROHM ADPCM | ROHM ADPCM  | o | o | - | |
| - | VOX ADPCM | VOX ADPCM  | o | o | - | |
| - | YAMAHA ADPCM | YAMAHA ADPCM  | o | o | - | |
| - | YM2068 ADPCM | YAMAHA ADPCM  | o | o | - | |
| sampled | ssrc | resampling | o | - | o | waiting for phase 1, TODO use nio pipe |

### Legend ###

|Mark|Meaning|
|:--|:---|
| o | ok |
| ? | not tested |
| c | under construction |
| x | ng |
| - | n/a |
