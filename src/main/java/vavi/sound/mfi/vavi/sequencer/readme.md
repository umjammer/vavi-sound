# vavi.sound.mfi.vavi.sysex

Provides sub classes of SysexMessage.

`SmafExclusive` is the way out of here: a machine dependent function which has
something a MIDI synthesizer can use (so far the NEC tone and wave messages)
builds the SMAF (Yamaha) exclusive that says the same thing and sends it to the
`Receiver` it is given, packed the way
`vavi.sound.smaf.message.yamaha.YamahaMessage` packs one while a SMAF file plays.
See `vavi.sound.mfi.vavi.nec` readme, "handing the voices to the synthesizer".

## TODO

 * there is still room for improvement
