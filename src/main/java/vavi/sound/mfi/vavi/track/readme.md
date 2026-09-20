# vavi.sound.mfi.vavi.track

Provides a derived class of SysexMessage.

### Δtime

  * Tempo ... number of quarter notes in 1 minute
  * Timebase ... Quarter note resolution = PPQ (Parts ...), TpQN (Tick par Quarter Note)
```
        1Δ = 60[s] / Tempo / Timebase;
```

### Chunks



#### -------- Note --------




#### -------- Normal (Extended status information) --------

##### ---- Extended status A ----

 All reserved

##### ---- Extended status B ----

 0xb# Sound source control information independent of channel numbers


| 0xb# | function                         | id        | class                       |
|------|----------------------------------|-----------|-----------------------------|
| 0xb0 | Master volume                    | 255.b.176 | MasterVolumeMessage         |
| 0xb1 | Master balance                   | 255.b.177 |                             |
| 0xbc | Relative change in tempo         | 255.b.188 |                             |
| 0xbd | Relative change in master volume | 255.b.189 |                             |
| 0xbe | Stop and mute                    | 255.b.190 |                             |
| 0xbf | Sound source reset               | 255.b.191 |                             |
| 0xba | Channel configuration            | 255.b.186 | ChannelConfigurationMessage |

 0xc# tempo message

| 0xc# | value | id        | class        |
|------|-------|-----------|--------------|
| 0xc0 | 6     | 255.b.192 | TempoMessage |
| 0xc1 | 12    | 255.b.193 | TempoMessage |
| 0xc2 | 24    | 255.b.194 | TempoMessage |
| 0xc3 | 48    | 255.b.195 | TempoMessage |
| 0xc4 | 96    | 255.b.196 | TempoMessage |
| 0xc5 | 192   | 255.b.197 | TempoMessage |
| 0xc6 | 384   | 255.b.198 | TempoMessage |
| 0xc7 | 予約    | 255.b.199 | TempoMessage |
| 0xc8 | 15    | 255.b.200 | TempoMessage |
| 0xc9 | 30    | 255.b.201 | TempoMessage |
| 0xca | 60    | 255.b.202 | TempoMessage |
| 0xcb | 120   | 255.b.203 | TempoMessage |
| 0xcc | 240   | 255.b.204 | TempoMessage |
| 0xcd | 480   | 255.b.205 | TempoMessage |
| 0xce | 960   | 255.b.206 | TempoMessage |
| 0xcf | 予約    | 255.b.207 | TempoMessage |

 0xd# Performance management information

| 0xd# | function                                     | id        | class             |
|------|----------------------------------------------|-----------|-------------------|
| 0xd0 | Performance position information (cue point) | 255.b.208 | CuePointMessage   |
| 0xdc | NOP2 (No OPeration2)                         | 255.b.220 | Nop2Message       |
| 0xdd | Loop point                                   | 255.b.221 | LoopPointMessage  |
| 0xde | NOP (No OPeration)                           | 255.b.222 | NopMessage        |
| 0xdf | End Of Track                                 | 255.b.223 | EndOfTrackMessage |

 0xe# Sound source control information

| 0xe# | function                  | id        | class                  |
|------|---------------------------|-----------|------------------------|
| 0xe0 | Tone number change        | 255.b.224 | ChangeVoiceMessage     |
| 0xe1 | Change tone bank          | 255.b.225 | ChangeBankMessage      |
| 0xe2 | Volume change             | 255.b.226 | VolumeMessage          |
| 0xe3 | Pan pot                   | 255.b.227 | PanpotMessage          |
| 0xe4 | Pitch bend settings       | 255.b.228 | PitchBendMessage       |
| 0xe5 | Channel assignment        | 255.b.229 | ChannelChangeMessage   |
| 0xe6 | Relative change in volume | 255.b.230 | ExpressionMessage      |
| 0xe7 | Pitch bend range setting  | 255.b.231 | PitchBendRangeMessage  |
| 0xe8 | Fine pitch bend A setting | 255.b.232 | PitchBendFineAMessage  |
| 0xe9 | Fine pitch bend B setting | 255.b.233 | PitchBendFineMessage   |
| 0xea | Modulation depth setting  | 255.b.234 | ModulationDepthMessage |

##### ---- Extended information ----

 0xf# Extended information


| 0xf# | function                             | id        | class                   | comment |
|------|--------------------------------------|-----------|-------------------------|---------|
| 0xf0 | VoiceEdit                            | 255.e.240 | VoiceEditMessage        | MFi1    |
| 0xf1 | ExtendedEdit                         | 255.e.241 | ExtendedEditMessage     | MFi1    |
| 0xf2 | KaraokeText                          | 255.e.242 |                         |         |
| 0xf3 | Karaoke                              | 255.e.243 |                         |         |
| 0xfe | Non-registered message specification | 255.e.254 |                         |         |
| 0xff | Expansion information                | 255.e.255 | MachineDependentMessage |         |
```
    length = dis.readShort()
    id = dis.read();
    data ...
     0: Extension information ID
        7654 3210
        ~~~~ ~~~~
        |    +- 0001: DoCoMo
        |       else: reserved
        +- 0000: reserved
            :
           1110: reserved
           1111: Expanded data for overseas
       If it is 0xff, you can specify the extension information identification ID2 next (1:).
     1: data or Extension information ID2
     2:  :
```

#### -------- Class A (Non-registered message) (0x3f) --------


##### ---- Extended status A ----

 All reserved

##### ---- Extended status B ----

 All reserved

##### ---- Extended information ----

 All reserved

##### -------- Class B (0x7f) --------


##### ---- Extended status A ----

| 0x7# | function   | id      | class            |
|------|------------|---------|------------------|
| 0x70 | Audio play | 127.a.0 | AudioPlayMessage |
| 0x71 | Audio Stop | 127.a.1 | AudioStopMessage |

##### ---- Extended status B ----

| 0x8# | function              | id        | class                     |
|------|-----------------------|-----------|---------------------------|
| 0x80 | Audio channel volume  | 127.b.128 | AudioChannelVolumeMessage |
| 0x81 | Audio channel pan pot | 127.b.129 | AudioChannelPanpotMessage |

##### 0x9#

| 0x8# | function                                   | id | class |
|------|--------------------------------------------|----|-------|
| 0x90 | 3D localization function usage information |    |       |

#### ---- Extended information ----

| 0xf# | function                      | id | class |
|------|-------------------------------|----|-------|
| 0xf0 | 3D localization information   |    |       |

### -------- Class C (0xbf) --------

#### ---- Extended status A ----

 All reserved

#### ---- Extended status B ----

 All reserved

#### ---- Extended information ----

 All reserved

### Meta event

| 0xe# | function           | id       | class        |
|------|--------------------|----------|--------------|
| 0x01 | Text               | meta.1   | ProtMessage  |
| 0x02 | Copyright          | meta.2   | CopyMessage  |
| 0x03 | Name               | meta.3   | TitlMessage  |
| 0x06 | Maker              | meta.6   | SuptMessage  |
| 0x51 | Set tempo          | 255.b.*  | TempoMessage |
| 0x58 | Beat               |          |              |
| 0x59 | Key signature      |          |              |
| 0x7f | Sequencer specific | meta.127 | AinfMessage  |

### Channel message

| 0x## |      | function                             | id        | class                 |
|------|------|--------------------------------------|-----------|-----------------------|
| 0x8_ |      | Note off                             | short.128 | VaviNoteMessage       |
| 0x9_ |      | Note on                              | short.144 | VaviNoteMessage       |
| 0xa_ |      | Polyphonic key pressure              |           |                       |
| 0xb_ |      | Control change                       |           |                       |
| 0xb_ | 0x00 | Bank select MSB                      | 176.0     | MfiConvertibleMessage |
| 0xb_ | 0x01 | Modulation depth MSB                 |           |                       |
| 0xb_ | 0x02 | Breath controller MSB                |           |                       |
| 0xb_ | 0x04 | Foot controller MSB                  |           |                       |
| 0xb_ | 0x05 | Portamento time MSB                  |           |                       |
| 0xb_ | 0x06 | Data entry MSB                       | 176.6     | MfiConvertibleMessage |
| 0xb_ | 0x07 | Main volume MSB                      |           |                       |
| 0xb_ | 0x08 | Balance control MSB                  |           |                       |
| 0xb_ | 0x0a | Pan pot MSB                          |           |                       |
| 0xb_ | 0x0b | Expression MSB                       |           |                       |
| 0xb_ | 0x20 | Bank select LSB                      | 176.32    | MfiConvertibleMessage |
| 0xb_ | 0x30 | General-purpose controller-1         |           |                       |
| 0xb_ | 0x31 | General-purpose controller-2         |           |                       |
| 0xb_ | 0x32 | General-purpose controller-3         |           |                       |
| 0xb_ | 0x33 | General-purpose controller-4         |           |                       |
| 0xb_ | 0x40 | Hold 1 (damper)                      |           |                       |
| 0xb_ | 0x41 | Portamento                           |           |                       |
| 0xb_ | 0x42 | Sostenuto                            |           |                       |
| 0xb_ | 0x43 | Soft pedal                           |           |                       |
| 0xb_ | 0x44 | Legato foot switch                   |           |                       |
| 0xb_ | 0x45 | Hold 2 (freeze)                      |           |                       |
| 0xb_ | 0x46 | Sound controller 1 (sound variation) |           |                       |
| 0xb_ | 0x47 | Resonance                            |           |                       |
| 0xb_ | 0x48 | Release time                         |           |                       |
| 0xb_ | 0x49 | Attack time                          |           |                       |
| 0xb_ | 0x4a | Cut off                              |           |                       |
| 0xb_ | 0x4b | Decay time                           |           |                       |
| 0xb_ | 0x4c | Vibrato trait                        |           |                       |
| 0xb_ | 0x4d | Vibrato depth                        |           |                       |
| 0xb_ | 0x4e | Vibrato delay                        |           |                       |
| 0xb_ | 0x50 | General-purpose controller-5         |           |                       |
| 0xb_ | 0x51 | General-purpose controller-6         |           |                       |
| 0xb_ | 0x52 | General-purpose controller-7         |           |                       |
| 0xb_ | 0x53 | General-purpose controller-8         |           |                       |
| 0xb_ | 0x54 | Portamento time LSB                  |           |                       |
| 0xb_ | 0x5b | General-purpose effect 1 (reverb)    |           |                       |
| 0xb_ | 0x5c | General-purpose effect 2 (tremolo)   |           |                       |
| 0xb_ | 0x5d | General-purpose effect 3 (chorus)    |           |                       |
| 0xb_ | 0x5e | General-purpose effect 4 (celeste)   |           |                       |
| 0xb_ | 0x5f | General-purpose effect 5 (phaser)    |           |                       |
| 0xb_ | 0x60 | Data increment                       |           |                       |
| 0xb_ | 0x61 | Data decrement                       |           |                       |
| 0xb_ | 0x62 | NRPN LSB                             | 176.98    | MfiConvertibleMessage |
| 0xb_ | 0x63 | NRPN MSB                             | 176.99    | MfiConvertibleMessage |
| 0xb_ | 0x64 | RPN LSB                              | 176.100   | MfiConvertibleMessage |
| 0xb_ | 0x65 | RPN MSB                              | 176.101   | MfiConvertibleMessage |

### + 0xb_ Channel mode message

| 0xb# |            | function                  | id | class |
|------|------------|---------------------------|----|-------|
| 0xb_ | 0x78 (120) | All sound off             |    |       |
| 0xb_ | 0x79 (121) | Rest all controller       |    |       |
| 0xb_ | 0x7a (122) | Local control             |    |       |
| 0xb_ | 0x7b (123) | All note off              |    |       |
| 0xb_ | 0x7c (124) | Omni off                  |    |       |
| 0xb_ | 0x7d (125) | Omni on                   |    |       |
| 0xb_ | 0x7e (126) | Mono on                   |    |       |
| 0xb_ | 0x7f (127) | Poly on                   |    |       |
| 0xc_ |            | Program change            |    |       |
| 0xd_ |            | Channel pressure          |    |       |
| 0xe_ |            | Pitch bend change         |    |       |
| 0xf0 |            | System exclusive message  |    |       |
| 0xf_ |            | System common message     |    |       |
| 0xf1 |            | MTC Quarter frame         |    |       |
| 0xf2 |            | Song position pointer     |    |       |
| 0xf3 |            | Song selection            |    |       |
| 0xf6 |            | Tune request              |    |       |
| 0xf7 |            | End of exclusive (EOX)    |    |       |
| 0xf_ |            | System real-time messages |    |       |
| 0xf8 |            | Timing clock              |    |       |
| 0xfa |            | Start                     |    |       |
| 0xfb |            | Continue                  |    |       |
| 0xfc |            | Stop                      |    |       |
| 0xfe |            | Active sensing            |    |       |
| 0xff |            | System reset              |    |       |

## TODO

 * Handling notes with Δ > 255

For example, tone 380 (voice = 1)

| delta | voice |         gateTime         |        explanation        |
|:-----:|:-----:|:------------------------:|:-------------------------:|
|   -   |   1   |     2 + 10 + 1 = 13      |                           |
|   2   |   2   |            -             |                           |
|  10   |   3   |            -             |                           |
|   0   |   1   | max(380 - 13, 255) = 255 | After one round of voice? |

PsmPlayer subtracts the skipped amount from the initial Δ

 * Information for MFi 5.0 extension

## TODO

- check `MachineDependentMessage` reading part
