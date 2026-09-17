# vavi.soudn.smaf.vavi.sequencer

Provides sub classes of SysexMessage.

### Status

- 0x45: vendor vavi ... not official, hijacked `unused`

| id | length | function group id | type           | description    | data issuer      | data                      | data processor    |
|----|--------|-------------------|----------------|----------------|------------------|---------------------------|-------------------|
| 45 | 4      | 03                | SMAF           | for adpcm      | SmafMessageStore | idH idL f7                | SmafReceiver      |
| 45 | -      | 7f                | PACKED         | for smaf sysex | YamahaExclusive  | 7bit packed data  ...  f7 | NuledOpl3Receiver |
