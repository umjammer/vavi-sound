# vavi.sound.adpcm.ima

Provides IMA ADPCM codec related classes.

## References

 * https://github.com/philburk/listenup/blob/master/src/com/softsynth/javasonics/util/ADPCM_IMA_Encoder.java

### License

```
/*
        ima_rw.c -- codex utilities for WAV_FORMAT_IMA_ADPCM

        Copyright (C) 1999 Stanley J. Brooks &lt;stabro@megsinet.net&gt;

    This library is freimport javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;

public class AudioCompressor {
    public static void main(String[] args) {
        byte[] audioData = ...; // Assume this is populated with audio data
        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            8000.0F, // Sample rate
            16, // Sample size in bits
            1, // Channels (mono)
            2, // Frame size
            8000.0F, // Frame rate
            false); // Big endian

        AudioInputStream ais = new AudioInputStream(
            new ByteArrayInputStream(audioData),
            format,e software; you can redistribute it and/or
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

## TODO

* ~~111016 stop reading at one~~
* ~~111016 ↑or use EngineeringIO~~
* ~~060121 `bdiff linear_8k_16_mono.pcm, out.vavi.pcm` are a bit different~~
* ~~030831 not play all?~~ → SourceDataLine#drain()
* ~~111101 encode 111031 a little more, initial value?~~
* tritonus WaveAudioFileReader has ima adpcm detection
* tritonus has ima adpcm coverter (ImaAdpcmFormatConversionProvider)
