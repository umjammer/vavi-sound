# vavi.sound.mfi.vavi.sysex

Provides sub classes of SysexMessage.

### Status

- 0x45: vendor vavi ... not official, hijacked `unused`

| id | length | function group id | type              | description       | data issuer                         | data                      | data processor    |
|----|--------|-------------------|-------------------|-------------------|-------------------------------------|---------------------------|-------------------|
| 45 | 4      | 01                | MACHINE_DEPENDENT | for adpcm         | MfiMessageStore                     | idH idL f7                | VaviReceiver      |
| 45 | 4      | 02                | MFi4              | for adpcm         | MfiMessageStore                     | idH idL f7                | VaviReceiver      |
| 45 | 4      | 03                | SMAF              | for adpcm         | SmafMessageStore                    | idH idL f7                | SmafReceiver      |
| 45 | -      | 04                | FUETREK           | for fuetrek sysex | FuetrekMfiExclusive                 | subId ... f7              | UcsReceiver       |
| 45 | -      | 7f                | PACKED            | for smaf sysex    | YamahaMfiExclusive, YamahaExclusive | 7bit packed data  ...  f7 | NuledOpl3Receiver |

### Note

`YamahaMfiExclusive` is the way out of here: a machine dependent function which has
something a MIDI synthesizer can use (so far the NEC tone and wave messages)
builds the SMAF (Yamaha) exclusive that says the same thing and sends it to the
`Receiver` it is given, packed the way
`vavi.sound.smaf.vavi.yamaha.message.YamahaMessage` packs one while a SMAF file plays.
See `vavi.sound.mfi.vavi.nec` readme, "handing the voices to the synthesizer".

## TODO

 * there is still room for improvement
