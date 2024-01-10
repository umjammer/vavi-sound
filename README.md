[![Release](https://jitpack.io/v/umjammer/vavi-sound.svg)](https://jitpack.io/#umjammer/vavi-sound)
[![Java CI](https://github.com/umjammer/vavi-sound/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-sound/actionsworkflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-sound/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-sound/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-sound

Provides old school Japanese cell phone sounds library as `javax.sound` SPI.
Includes many ADPCM codecs and the [SSRC](https://github.com/shibatch/SSRC) sampling rate converter.

## Status

| **SPI** | **Codec**                                                |  **Description**           | **IN Status** | **OUT Status** | **SPI Status** | **Comment**                 |
|:--------|:---------------------------------------------------------|:---------------------------|:--------------|:---------------|:---------------|:----------------------------|
| midi    | [MFi](src/main/java/vavi/sound/midi/mfi)                 | Japanese cell phone format | 🚧 | ✅ | ✅ |                             |
| midi    | [SMAF](src/main/java/vavi/sound/midi/smaf)               | YAMAHA cell phone format   | 🚧 | ✅ | ✅ |                             |
| sampled | [MFi](src/main/java/vavi/sound/sampled/mfi)              | Japanese cell phone format | ✅ | ✅ | ✅ |                             |
| sampled | [SMAF](src/main/java/vavi/sound/sampled/smaf)            | YAMAHA cell phone format   | ✅ | ✅ | ✅ |                             |
| sampled | [CCITT ADPCM](src/main/java/vavi/sound/adpcm/ccitt)      | G711, G721, G723           | ✅ | ✅ | ✅ |                             |
| sampled | [DVI ADPCM](src/main/java/vavi/sound/adpcm/dvi)          | DVI ADPCM                  | ✅ | ✅ | ✅ |                             |
| sampled | [IMA ADPCM](src/main/java/vavi/sound/adpcm/ima)          | IMA ADPCM                  | ✅ | ✅ | ✅ |                             |
| sampled | [MA ADPCM](https://gitlab.com/umjammer/vavi-sound-nda)   | YAMAHA ADPCM               | ✅ | ✅ | ✅ |                             |
| sampled | [MS ADPCM](src/main/java/vavi/sound/adpcm/ms)            | Microsoft ADPCM            | ✅ | ✅ | ✅ |                             |
| sampled | [OKI ADPCM](src/main/java/vavi/sound/adpcm/oki)          | OKI ADPCM                  | ✅ | ✅ | ✅ |                             |
| sampled | [ROHM ADPCM](https://gitlab.com/umjammer/vavi-sound-nda) | ROHM ADPCM                 | ✅ | ✅ | ✅ |                             |
| sampled | [VOX ADPCM](src/main/java/vavi/sound/adpcm/vox)          | VOX ADPCM                  | ✅ | ✅ | ✅ |                             |
| sampled | [YAMAHA ADPCM](src/main/java/vavi/sound/adpcm/yamaha)    | YAMAHA ADPCM               | ✅ | ✅ | - | same as ym2068              |
| sampled | [YM2068 ADPCM](src/main/java/vavi/sound/adpcm/ym2608)    | YAMAHA ADPCM               | ✅ | ✅ | ✅ |                             |
| sampled | [ssrc](src/main/java/vavi/sound/pcm/resampling/ssrc)     | resampling                 | ✅ | -  | ✅ | need to wait for phase 1 |

## Install

 * https://jitpack.io/#umjammer/vavi-sound

## FAQ

#### Q. can I use SSRC sampling converter under LGPL license?

A. yes you can, follow those steps

 * create a separated jar (ssrc.jar) file including ssrc classes. (**never include those .class files into your application jar file**)
   * `vavi/sound/pcm/resampling/ssrc/SSRC.class`
   * `vavi/util/SplitRadixFft.class`
   * `vavi/util/I0Bessel.class`
 * ⚠ **caution**:
   * your application complies with the LGPL. customers **have a right to reverse engineering your application**.
   * if you include ssrc.jar with a distribution, you **must offer a way to get ssrc source code**.
 * see also
   * https://opensource.org/licenses/LGPL-2.1
   * http://www.gnu.org/licenses/lgpl-java.en.html

## Tech Know

 * github actions workflow on ubuntu java8 cannot deal line `PCM_SIGNED 8000.0 Hz, 16 bit, mono, 2 bytes/frame, little-endian`

## TODO

  * use `Receiver` instead of `MetaEventListener`
  * ssrc: use nio pipe for 1st pass
    * on macos m2 ultra 1st pass is in a blink of an eye
  * ~~`ima`, `ms` adpcm: wav reader~~
    * ~~`tritonus:tritonus-remaining:org.tritonus.sampled.file.WaveAudioFileReader`~~
    * ~~wip at [vavi-sound-sandbox](https://github.com/umjammer/vavi-sound-sandbox)~~ done in this project
