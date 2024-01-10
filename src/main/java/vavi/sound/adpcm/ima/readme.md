# vavi.sound.adpcm.ima

Provides IMA ADPCM codec related classes.

## TODO

 * ~~111016 stop reading at one~~
 * ~~111016 ↑or use EngineeringIO~~
 * ~~060121 `bdiff linear_8k_16_mono.pcm, out.vavi.pcm` are a bit different~~
 * ~~030831 not play all?~~ → SourceDataLine#drain()
 * ~~111101 encode 111031 a little more, initial value?~~
 * tritonus WaveAudioFileReader has ima adpcm detection
 * tritonus has ima adpcm coverter (ImaAdpcmFormatConversionProvider)

## License

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