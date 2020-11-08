IMA ADPCM フォーマット関連のクラスを提供します．

## TODO

 * ~~111016 一括読み込みを止める~~
 * ~~111016 ↑もしくは EngineeringIO を使う~~
 * ~~060121 ちょっと違うところがある bdiff linear_8k_16_mono.pcm, out.vavi.pcm~~
 * ~~030831 全部再生されない？~~ → SourceDataLine#drain()
 * ~~111101 encode 111031 あとちょっと、初期値とか？~~
 * tritonus WaveAudioFileReader has ima adpcm detection
 * tritonus has ima adpcm coverter (ImaAdpcmFormatConversionProvider)

## License

<b style="color:red">WARNING THIS PROGRAM LICENCE IS LGPL</b>

```
/*
        ima_rw.c -- codex utilities for WAV_FORMAT_IMA_ADPCM

        Copyright (C) 1999 Stanley J. Brooks &lt;stabro@megsinet.net&gt;

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

*/
```