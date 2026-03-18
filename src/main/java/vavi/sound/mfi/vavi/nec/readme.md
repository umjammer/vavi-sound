# vavi.sound.mfi.vavi.nec

Provides NEC-specific message processing classes.

- f0._1 FM voice change
- f0._2 ADPCM data
- f1._0 ADPCM on
- f1._1 Vibrato
- f1._2 pitch bend
- f2._1 ADPCM volume
- f2._4 vibrator
- f2._5 LED

- 01.f0._3 Extended FM basic waveform
- 01.f0._4 Extended FM tone specification
- 01.f0._5 Extended WT tone specification
- 01.f0._6 Extended WT waveform specification
- 01.f0._7 Extended stream waveform control information
- 01.f0._8 Extended AL tone specification

- 01.f1.x3 StreamOn
- 01.f1.x4 StreamSlaveOn
- 01.f1.x5 StreamOff
- 01.f1.x6 StreamPan
- 01.f1.x7 Hold1
- 01.f1.x8 MonoOn
- 01.f1.x9 PlayOn
- 01.f1.xa filter resonance
- 01.f1.xb filter brightness

- 01.f2._7 Channel Status control information

- 01.f3._1 FM mode setting

- 01.f3._2 UserEvent
- 01.f3._3 MaxGain setting
- 01.f3._4 Specify number of streams
- 01.f3._5 AL channel specification
-
- 02.f0.06 ADPCM data
- 01.f1.0d StreamOn

## TODO

 * for MA7, function 2.0, 2.1, 2.2, 2.3 are undefined

tmp/samples/n703id/02 TRANSPARENT.mld

| id      | len | comment    |
|---------|-----|------------|
| 2.f3.0a | 1   | 01 id?     |
| 2.f2.07 | 32  | ch?        |
| 2.f3.03 | 1   | 06 ?       |
| 2.f0.06 | n/a | audio data |
| 2.f0.0c | 38  | fm?        |
| 2.f0.0c | 64  | fm?        |
| 2.f1.0d | 1   | 7f vol?    |
