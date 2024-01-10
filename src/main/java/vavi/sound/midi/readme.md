# vavi.sound.midi

Provides base classes for the MIDI service provider.

## 注意
MFi, SMAF をこのパッケージで再生する場合、システムプロパティ `javax.sound.midi.Sequencer` に
`"#Java MIDI(MFi/SMAF) ADPCM Sequencer"` と設定してください。ADPCM のハンドリングができなくなります。

## MIDI file type for MFi/SMAF (⚠ this is not MIDI specs, proprietary)

```
 8765 4321 LSB
 ||   |||+---- SMF 0/1
 ||   ||+----- SMF 2
 ||   |+------ SMAF
 ||   +------- MFi
 |+----------- Compress Flag (SMAF)
 +------------ for Mobile Flag (SMAF, MFi etc)
```

## TODO

 * implement SMAF Handyphone/Mobile
 * ↓ is OK
   ```java
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        Sequence sequence = MidiSystem.getSequence(is);
        sequencer.setSequence(sequence);
   ```
   ↓ is NG, Smaf, `MfiFileReader` returns (after second providers?) 0 byte `ByteArrayInputStream` MIDI stream
   ```java
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        sequencer.setSequence(is);
   ```
 * ~~implement `DeveiceInfo` properly (`MidiSystem#getSequencer()` returns `SmafMidiSequencer`(first))~~ → specs

## Deprecated

```java
            File soundprops = new File(
                System.getProperty("java.home") +
                File.separator + "lib",
                "sound.properties");
Debug.println("soundprops: " + soundprops);
            String defaultPropertyValue;
            if (soundprops.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(soundprops));
                defaultPropertyValue = props.getProperty("javax.sound.midi.Sequencer", defaultSequencer);
            } else {
                defaultPropertyValue = defaultSequencer; 
            }
Debug.println("defaultPropertyValue: " + defaultPropertyValue);

            String propertyValue = System.getProperty("javax.sound.midi.Sequencer", defaultPropertyValue);
```
