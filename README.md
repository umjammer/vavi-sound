[![Release](https://jitpack.io/v/umjammer/vavi-sound.svg)](https://jitpack.io/#umjammer/vavi-sound)
[![Java CI](https://github.com/umjammer/vavi-sound/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-sound/actionsworkflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-sound/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-sound/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-sound

<img alt="logo" src="https://github.com/umjammer/vavi-sound/assets/493908/7a731744-643a-4b6c-b82b-68f2fcc436c9" width="160" />

Provides old school Japanese cell phone sounds library as `javax.sound(.midi)` SPI<br/>
includes many ADPCM codecs and the [SSRC](https://github.com/shibatch/SSRC) sampling rate converter.

### Status

| **SPI** | **Codec**                                                | **Description**           | **IN Status** | **OUT Status** |  **SPI Status**  | **Comment**                    |
|:--------|:---------------------------------------------------------|:--------------------------|:-------------:|:--------------:|:----------------:|:-------------------------------|
| midi    | [MFi](src/main/java/vavi/sound/midi/mfi)                 | Japanese ring tone format |      🚧       |       ✅        |        ✅         | DoCoMo                         |
| midi    | [SMAF](src/main/java/vavi/sound/midi/smaf)               | YAMAHA ring tone format   |      🚧       |       ✅        |        ✅         | au, Softbank                   |
| sampled | [MFi](src/main/java/vavi/sound/sampled/mfi)              | Japanese ring tone format |       ✅       |       ✅        |        ✅         | DoCoMo                         |
| sampled | [SMAF](src/main/java/vavi/sound/sampled/smaf)            | YAMAHA ring tone format   |       ✅       |       ✅        |        ✅         | au, Softbank                   |
| sampled | [CCITT ADPCM](src/main/java/vavi/sound/adpcm/ccitt)      | G711, G721, G723          |       ✅       |       ✅        |        ✅         | G721 cellphone w/ Fuetrek chip |
| sampled | [DVI ADPCM](src/main/java/vavi/sound/adpcm/dvi)          | DVI ADPCM                 |       ✅       |       ✅        |        ✅         |                                |
| sampled | [IMA ADPCM](src/main/java/vavi/sound/adpcm/ima)          | IMA ADPCM                 |       ✅       |       ✅        | ✅ <sup>[1]</sup> |                                |
| sampled | [MA ADPCM](https://gitlab.com/umjammer/vavi-sound-nda)   | YAMAHA ADPCM              |       ✅       |       ✅        |        ✅         | cellphone w/ YAMAHA MA chip    |
| sampled | [MS ADPCM](src/main/java/vavi/sound/adpcm/ms)            | Microsoft ADPCM           |       ✅       |       ✅        | ✅ <sup>[1]</sup> |                                |
| sampled | [OKI ADPCM](src/main/java/vavi/sound/adpcm/oki)          | OKI ADPCM                 |       ✅       |       ✅        | ✅ <sup>[1]</sup> |                                |
| sampled | [ROHM ADPCM](https://gitlab.com/umjammer/vavi-sound-nda) | ROHM ADPCM                |       ✅       |       ✅        |        ✅         | cellphone w/ Rohm chip         |
| sampled | [VOX ADPCM](src/main/java/vavi/sound/adpcm/vox)          | VOX ADPCM                 |       ✅       |       ✅        | ✅ <sup>[1]</sup> |                                |
| sampled | [YAMAHA ADPCM](src/main/java/vavi/sound/adpcm/yamaha)    | YAMAHA ADPCM              |       ✅       |       ✅        | ✅ <sup>[1]</sup> |                                |
| sampled | [YM2068 ADPCM](src/main/java/vavi/sound/adpcm/ym2608)    | YAMAHA ADPCM              |       ✅       |       ✅        |        -         | same as YAMAHA ADPCM           |
| sampled | [ssrc](src/main/java/vavi/sound/pcm/resampling/ssrc)     | resampling                |       ✅       |       -        |        ✅         | need to wait for phase 1       |

<sub>[1] wav file readable</sub>

## Install

* https://jitpack.io/#umjammer/vavi-sound

## Usage

### sample

 * MFi (.mld) ... [PlayMFi](src/test/java/PlayMFi.java)
 * SMAF (.mmf) ... [PlaySMAF](src/test/java/PlaySMAF.java)

### FAQ

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

### Tech Know

* \[github actions] workflow on ubuntu java8 cannot deal line `PCM_SIGNED 8000.0 Hz, 16 bit, mono, 2 bytes/frame, little-endian`
* \[midi volume] avoiding noise, `SoundUtil#volume` should be called before `Sequencer#setSequence`

## References

 * https://github.com/shibatch/SSRC

## TODO

  * use `Receiver` instead of `MetaEventListener`
  * ssrc: use nio pipe for 1st pass
    * on macos m2 ultra 1st pass is in a blink of an eye
  * ~~`ima`, `ms` adpcm: wav reader~~
    * ~~`tritonus:tritonus-remaining:org.tritonus.sampled.file.WaveAudioFileReader`~~
  * use service provider for mfi, smaf sequencer
  * service loader instead of vavi.properties
  * [RTTTL (Ringing Tones text transfer language)](https://web.archive.org/web/20070704033948/http://www.convertyourtone.com/rtttl.html)
  * https://github.com/SatyrDiamond/adpcm
  * midi -> smaf

---
<sub>images by <a href="https://www.silhouette-illust.com/illust/49312">melody</a>, <a href="https://www.silhouette-illust.com/illust/257">cellphone</a></sub>
