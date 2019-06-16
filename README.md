[![Release](https://jitpack.io/v/umjammer/vavi-sound.svg)](https://jitpack.io/#umjammer/vavi-sound)

# vavi-sound

Provides old school Japanese cell phone sounds library as `javax.sound` SPI.
Includes many ADPCM codecs and the SSRC sampling converter.

| **SPI** |  **Codec** |  **Description** | **IN Status** | **OUT Status** | **SPI Status** | **Comment** |
|:--------|:-----------|:-----------------|:--------------|:---------------|:---------------|:------------|
| midi | MFi | Japanese cell phone format |  🚧 | ✅ | ✅ | spi write only |
| midi | SMAF | YAMAHA cell phone format | 🚧 | ✅ | ✅ | spi write only |
| sampled | MFi | Japanese cell phone format |  🚫 | ✅ | 🚫 | |
| sampled | SMAF | YAMAHA cell phone format | ✅ | ✅ | ✅ | |
| sampled | CCITT ADPCM | G711, G721, G723 | ✅ | ✅ | 🚫 | |
| - | DVI ADPCM | DVI ADPCM | ✅  | ✅ | - | |
| - | IMA ADPCM | IMA ADPCM  | ✅ | ✅ | - | |
| - | MA ADPCM | YAMAHA ADPCM  | ✅  | ✅ | - | |
| - | MS ADPCM | Microsoft ADPCM  | ✅  | ✅ | - | |
| - | OKI ADPCM | OKI ADPCM  | ✅ | ✅ | - | |
| - | ROHM ADPCM | ROHM ADPCM  | ✅ | ✅ | - | |
| - | VOX ADPCM | VOX ADPCM  | ✅ | ✅ | - | |
| - | YAMAHA ADPCM | YAMAHA ADPCM  | ✅ | ✅ | - | |
| - | YM2068 ADPCM | YAMAHA ADPCM  | ✅ | ✅ | - | |
| sampled | ssrc | resampling | ✅ | - | ✅ | waiting for phase 1, TODO use nio pipe |

### Legend ###

|Mark|Meaning|
|:--|:---|
| ✅ | ok |
| ? | not tested |
| 🚧 | under construction |
| 🚫 | ng |
| - | n/a |
