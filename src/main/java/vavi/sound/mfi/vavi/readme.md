# vavi.sound.mfi.vavi

Provides a class for MFi service provider implementation example.

 * As a special specification, the empty number 0xfd in the extension information is used as MetaMessage.

## TODO

 * ~~020630 VaviSequencer: stop()~~
 * ~~020706 VaviSequence: Midi Converter~~
 * ~~030826 Create a MidiFileReader~~
 * ~~030917 Some InfoMessages can be converted to MetaMessage (1,2,3...)~~
 * ~~041223 TempoMessage must also support delta time~~
 * ~~041231 SMF type 1 compatible → Put 4ch into one ArrayList and then process it?~~
 * Should MFiConvertible be implemented somewhere else?
 * ADPCM also plays according to Δ time -> Thread?
 * SMF -> M$ type where MFi is still strange
 * ~~070117 Integration of { Normal, Class A, B, C, Note }, { Extension A, B, Info }~~
 * Track, AudioData, Info* integration
