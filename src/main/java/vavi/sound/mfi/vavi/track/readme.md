# vavi.sound.mfi.vavi.system

Provides a derived class of SysexMessage.

### Δtime

  * Tempo ... number of quarter notes in 1 minute
  * Timebase ... Quarter note resolution = PPQ (Parts ...), TpQN (Tick par Quarter Note)
```
        1Δ = 60[s] / Tempo / Timebase;
```

## TODO

 * Handling notes with Δ > 255

For example, tone 380 (voice = 1)

|delta|voice|gateTime|explanation|
|:-:|:-:|:-:|:-:|
|-|1|2 + 10 + 1 = 13||
|2|2|-||
|10|3|-||
|0|1|max(380 - 13, 255) = 255|After one round of voice?|

PsmPlayer subtracts the skipped amount from the initial Δ

 * Information for MFi 5.0 extension
