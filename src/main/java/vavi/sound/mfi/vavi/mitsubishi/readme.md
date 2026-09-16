# vavi.sound.mfi.vavi.mitsubishi

Provides message processing classes specific to Mitsubishi Electric.

- 0x60 ... Mitsubishi vendor id

| 0x8# | function                    | id     | class       |
|------|-----------------------------|--------|-------------|
| 0x01 | Pitch Bend setting          | 96.1   | Function1   |
| 0x02 | Pitch Bend Range setting    | 96.2   | Function2   |
| 0x03 | Modulation setting          | 96.3   | Function3   |
| 0x10 | UCS Wave setting            | 96.16  | Function16  |
| 0x11 | UCS Parameter setting       | 96.17  | Function17  |
| 0x12 | UCS Admin status setting    | 96.18  | Function18  |
| 0x81 | WAVE Channel Volume setting | 96.129 | Function129 |
| 0x82 | WAVE Channel Panpot setting | 96.130 | Function130 |
| 0x83 | WAVE Packet Data            | 96.131 | Function131 |
| 0x84 | WAVE Packet Data2           | 96.132 | Function132 |
| 0x85 | WAVE Packet Data3           | 96.133 | Function133 |
| 0x8f | Wave Setup                  | 96.143 | Function143 |

## TODO

- there are ucs settings!
