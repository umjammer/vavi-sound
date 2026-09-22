# vavi.sound

♪ provides about sound related classes.

 * vavi.sound.adpcm ... adpcm
 * vavi.sound.mfi ... i-melody
 * vavi.sound.smaf ... smaf
 * vavi.sound.pmd ... au pmd (cmx) reader, midi converter
 * vavi.sound.dxm ... feelsound dxm reader, midi converter
 * vavi.sound.midi ... i-melody and smaf adpcm
 * vavi.sound.mobile ... mobile adpcm
 * vavi.sound.sampled.ssrc ... sampling rate converter
 * vavi.sound.sampled.adpcm ... adpcm as spi

## References

 - https://lpcwiki.miraheze.org/wiki/Ringtone_file_formats
 - https://github.com/wackypack/mtex
 - https://www.ssw.co.jp/support/contents/man/cmguide.htm
 - sample
   - mfm ... `~/.wine/drive_c/Program Files (x86)/Faith/Ring Tone Authoring Tool/Sample`
   - dxm ... `~/Public/np2/feel sound box (dxm)`
   - smd ... `~/Public/np2/smd`
   - pmd
     - http://mind.f.fiw-web.net/phonemelody/index.html (cell phone needed or user-agent?)
     - `~/Public/np2/SPH-A920`
 - https://3.onj.me/phonetones/

## TODO

- smd, smz ... j-phone
- dxm ... phs, feelsound, w/ adpcm, oki synthesizer
  - ~~reader~~ (sequence only), ~~converting to midi~~
- mfm ... fuetrek, w/ adpcm, like pmd
- pmd (cmx) ... au, CMIDI
  - ~~reader~~, ~~converting to midi~~, wave
