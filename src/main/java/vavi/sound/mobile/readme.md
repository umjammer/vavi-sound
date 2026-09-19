# vavi.sound.mobile

Provides a wave sequencer class for mobile.

## References

 - [mfi](../mfi/vavi/sequencer/AudioDataSequencer.java)
 - [smaf](../smaf/vavi/sequencer/WaveSequencer.java)

## Usage

### system properties

| property                               | default | what                                                             |
|----------------------------------------|---------|------------------------------------------------------------------|
| `vavi.sound.mobile.AudioEngine.output` | `line`  | `line`: each engine plays to a `SourceDataLine` of its own, timed by `AudioEngine.Sync` on the wall clock. `mixer`: no line; a start is a voice of `AudioEngineMixer` at once, on the caller's thread, and the player pulls the voices with `AudioEngineMixer#render` at its own rate |
| `vavi.sound.mobile.AudioEngine.volume` | `0.2`   | the line's gain, or what the mixer multiplies by                 |

`mixer` is for a player which renders its synthesizer itself (and mixes, records, pauses it):
send the messages that fall before a block, then render the synthesizer and
`AudioEngineMixer.render(buffer, offset, frames, rate)` over it.

## TODO

 * something is wrong with `continued` ... what's?
 * investigate yamaha Adpcm-A, B and MA#
 * ~~output not only to line but also to data~~ ... `-Dvavi.sound.mobile.AudioEngine.output=mixer`, see below
