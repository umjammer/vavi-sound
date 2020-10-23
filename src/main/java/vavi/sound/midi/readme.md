MIDI のサービスプロバイダとしての実装をするための基本クラスを提供します。

## 注意
MFi, SMAF をこのパッケージで再生する場合、システムプロパティ `javax.sound.midi.Sequencer` に
`"#Java MIDI(MFi/SMAF) ADPCM Sequencer"` と設定してください。ADPCM のハンドリングができなくなります。

## MIDI ファイルタイプの勝手な仕様

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

 * SMAF Handyphone/Mobile の切り分け
 * ↓は OK で
```java
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        Sequence sequence = MidiSystem.getSequence(is);
        sequencer.setSequence(sequence);
```
↓ は NG、Smaf, `MfiFileReader` (プロバイダ２つ目以降？)に
 0 バイトの `ByteArrayInputStream` の MIDI ストリームが渡ってくる
```java
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        sequencer.setSequence(is);
```
 * ~~`DeveiceInfo` 周りをちゃんとする (`MidiSystem#getSequencer()` で `SmafMidiSequencer`(一番目) が返る)~~ → 仕様

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
