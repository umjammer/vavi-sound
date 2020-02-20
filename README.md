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

## TechKnow

  * Tritonus mp3 はタグ無ししかサポートしていない
  * `javax.sound.midi.MidiUnavailableException: MIDI OUT transmitter not available` が出るのは JMF の sound.jar がクラスパス中にあるから

## TODO

  * ~~midi 激重どうにかならんのか？~~
  * Transcoder
  * ~~channels~~

## License

  * [Tritonus: Open Source Java Sound](http://www.tritonus.org/)

> Tritonus is distributed under the terms of the [GNU Library General Public License](http://www.gnu.org/copyleft/lesser.html)

  * [Java Sound Example](http://www.jsresources.org/)

> ```
> /*
>  * Copyright (c) 1999 - 2003 by Matthias Pfisterer
>  * All rights reserved.
>  *
>  * Redistribution and use in source and binary forms, with or without
>  * modification, are permitted provided that the following conditions
>  * are met:
>  *
>  * - Redistributions of source code must retain the above copyright notice,
>  *   this list of conditions and the following disclaimer.
>  * - Redistributions in binary form must reproduce the above copyright
>  *   notice, this list of conditions and the following disclaimer in the
>  *   documentation and/or other materials provided with the distribution.
>  *
>  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
>  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
>  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
>  * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
>  * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
>  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
>  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
>  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
>  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
>  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
>  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
>  * OF THE POSSIBILITY OF SUCH DAMAGE.
>  */
> ```
