[![Release](https://jitpack.io/v/umjammer/vavi-sound.svg)](https://jitpack.io/#umjammer/vavi-sound)
[![Java CI with Maven](https://github.com/umjammer/vavi-sound/workflows/Java%20CI%20with%20Maven/badge.svg)](https://github.com/umjammer/vavi-sound/actions)
[![CodeQL](https://github.com/umjammer/vavi-sound/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-sound/actions/workflows/codeql-analysis.yml)

# vavi-sound

Provides old school Japanese cell phone sounds library as `javax.sound` SPI.
Includes many ADPCM codecs and the SSRC sampling rate converter.

## Status

| **SPI** |  **Codec**   |  **Description**           | **IN Status** | **OUT Status** | **SPI Status** | **Comment** |
|:--------|:-------------|:---------------------------|:--------------|:---------------|:---------------|:------------|
| midi    | MFi          | Japanese cell phone format | 🚧 | ✅ | ✅ | |
| midi    | SMAF         | YAMAHA cell phone format   | 🚧 | ✅ | ✅ | |
| sampled | MFi          | Japanese cell phone format | ✅ | ✅ | ✅ | |
| sampled | SMAF         | YAMAHA cell phone format   | ✅ | ✅ | ✅ | |
| sampled | CCITT ADPCM  | G711, G721, G723           | ✅ | ✅ | ✅ | except `AudioFileReader` |
| sampled | DVI ADPCM    | DVI ADPCM                  | ✅ | ✅ | ✅ | except `AudioFileReader`  |
| sampled | IMA ADPCM    | IMA ADPCM                  | ✅ | ✅ | 🚧 | except `AudioFileReader`  |
| sampled | MA ADPCM     | YAMAHA ADPCM               | ✅ | ✅ | ✅ | except `AudioFileReader`  |
| sampled | MS ADPCM     | Microsoft ADPCM            | ✅ | ✅ | 🚧 | except `AudioFileReader`  |
| sampled | OKI ADPCM    | OKI ADPCM                  | ✅ | ✅ | ✅ | except `AudioFileReader`  |
| sampled | ROHM ADPCM   | ROHM ADPCM                 | ✅ | ✅ | ✅ | except `AudioFileReader`  |
| sampled | VOX ADPCM    | VOX ADPCM                  | ✅ | ✅ | ✅ | except `AudioFileReader`  |
| sampled | YAMAHA ADPCM | YAMAHA ADPCM               | ✅ | ✅ | - | same as ym2068 |
| sampled | YM2068 ADPCM | YAMAHA ADPCM               | ✅ | ✅ | ✅ | except `AudioFileReader`  |
| sampled | ssrc         | resampling                 | ✅ | -  | ✅ | waiting for phase 1 |

### Install

 * https://jitpack.io/#umjammer/vavi-sound

### FAQ

#### Q. can I use SSRC sampling converter under LGPL license?

A. yes you can, follow those steps

 * create a separated jar (ssrc.jar) file including ssrc classes. (**never include those .class files into your application jar file**)
   * `vavi/sound/pcm/resampling/ssrc/SSRC.class`
   * `vavi/util/SplitRadixFft.class`
   * `vavi/util/I0Bessel.class`
 * caution:
   * your application complies with the LGPL. customers **have a right to reverse engineering your application**.
   * if you include ssrc.jar with a distribution, you **must offer a way to get ssrc source code**.
 * see also
   * https://opensource.org/licenses/LGPL-2.1
   * http://www.gnu.org/licenses/lgpl-java.en.html

## Teck Know

 * github actions workflow on ubuntu java8 cannot deal line `PCM_SIGNED 8000.0 Hz, 16 bit, mono, 2 bytes/frame, little-endian`

## TODO

  * use `Receiver` instead of `MetaEventListener`
  * ssrc: use nio pipe
  * ima,ms adpcm: wav reader
