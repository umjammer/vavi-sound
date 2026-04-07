# vavi.sound.midi

Provides base classes for the MIDI service provider.

## Usage

### MIDI file type for MFi/SMAF (⚠️ this is not MIDI specs but proprietary)

```
 8765 4321 LSB
 ||   |||+---- SMF 0/1
 ||   ||+----- SMF 2
 ||   |+------ SMAF
 ||   +------- MFi
 |+----------- Compress Flag (SMAF)
 +------------ for Mobile Flag (SMAF, MFi etc)
```

### using built-in synthesizer

```java
        System.setProperty("javax.sound.midi.Synthesizer", "#Java MIDI(MFi) Synthesizer"); // for MFi
        System.setProperty("javax.sound.midi.Synthesizer", "#Java MIDI(SMAF) Synthesizer"); // for SMAF

        Sequencer sequencer = MidiSystem.getSequencer(false); // not connect to synthesizer
        sequencer.open();
        Synthesizer synthesizer = MidiSystem.getSynthesizer(); // synthesizer set by above
        synthesizer.open();
        sequencer.getTransmitter().setReceiver(synthesizer.getReceiver()); // adpcm player w/ default built-in synthesizer
        Sequence sequence = MidiSystem.getSequence(Files.newInputStream(Path.of(file)));
        sequencer.setSequence(sequence);
        sequencer.start();
```

### using own synthesizer w/ adpcm player

```java
        System.setProperty("javax.sound.midi.Synthesizer", "#your_synthesizer_name");

        Sequencer sequencer = MidiSystem.getSequencer(false); // not connect to synthesizer
        sequencer.open();
        Synthesizer synthesizer = MidiSystem.getSynthesizer(); // synthesizer set by above
        synthesizer.open();
        sequencer.getTransmitter().setReceiver(new SmafReceiver(synthesizer)); // adpcm player w/ your synthesizer
        Sequence sequence = MidiSystem.getSequence(Files.newInputStream(Path.of(file)));
        sequencer.setSequence(sequence);
        sequencer.start();
```

example

- [w/ opl3 synthesizer](https://github.com/umjammer/vavi-apps-mfiplayer)

### SMAF sysex

SMAF sysex data has 8bit data. converted to midi sysex message data is packed to 7bits.
you need to unpack data to 8bit.

```java
        // data is gotten by SysexMessage#getData()
        byte[] encoded = Arrays.copyOfRange(data, 2, data.length - 2); // 0xf0 0x45 {0x43 ...} 0x7f
        byte[] decoded = new byte[((encoded.length + 1) * 7) / 8]; // for 8bits data
        int n = decode87(encoded, decoded, 0, encoded.length);
        byte[] sysex = new byte[n + 1]; // for 8bits data + 0xf7
        System.arraycopy(decoded, 0, sysex, 0, n);
        sysex[sysex.length - 1] = data[data.length - 1]; // 0xf7
```

## TODO

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
