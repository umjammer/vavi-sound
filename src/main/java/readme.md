# `vavi.sound`

このドキュメントは着メロライブラリの API 仕様です。

## Feature

 * {@link javax.sound.midi.spi} として実装されているため特にプログラムで意識することなく MFi ファイルを midi ファイルとして取り扱い可能です。
 * MFi バージョン 3.1 までの形式をサポートしています。
 * MFi ←→ SMF の相互変換が可能です。
 * 機種依存データのハンドラを追加する事で各機種に対応した着声が作成可能です。

## Abstract

三つの大きなパートから構成されます。

 1. ADPCM codecs
 2. MFi library
 3. {@link javax.sound.midi.spi} implementation

### 1. ADPCM codecs

パッケージ

 * {@link vavi.sound.adpcm.ccitt}
 * {@link vavi.sound.adpcm.ma}
 * {@link vavi.sound.adpcm.vox}

着声に使用される ADPCM コーデックを取り扱うパッケージ群です。
Java の標準 IO ({@link java.io.InputStream}/{@link java.io.OutputStream}) として実装されているため
取り扱いが容易でストリーミングも楽に行えます。

### 2. MFi library

packages

 * {@link vavi.sound.mfi}
 * {@link vavi.sound.mfi.vavi}
 *   :

着メロライブラリの核となる部分です。MFi のファイル構造を取り扱い、MIDI 構造に変換する事が出来ます。
また MIDI から MFi への変換も可能です。基本的に {@link javax.sound.midi} パッケージと同じ構造をとっています。
機種依存データを扱うパッケージとして {@link vavi.sound.mfi.vavi.mitsubishi D社} と
{@link vavi.sound.mfi.vavi.nec N社} がサンプル実装されています。

### 3. {@link javax.sound.midi.spi} implementation

packages

 * {@link vavi.sound.midi}
 * {@link vavi.sound.midi.mfi}

{@link javax.sound.midi.spi} の一実装として機能させるためのパッケージです。
[SPI](http://java.sun.com/j2se/1.5.0/ja/docs/ja/guide/sound/programmer_guide/chapter1.html#111901)
仕様に従い登録すれば midi の一ファイル形式として MFi ファイルを
再生する事が可能になります。

## How to

### 1. Create MIDI from MFi

```java
            File inFile = new File("sample.mld");
            vavi.sound.mfi.Sequence mfiSequence = MfiSystem.getSequence(inFile);
            Sequence midiSequence = MfiSystem.toMidiSequence(mfiSequence);

            File outFile = new File("sample.mid");
            MidiSystem.write(midiSequence, 0, outFile);
```

### 2. Create MFi from MIDI

```java
            File inFile = new File("sample.mid");
            javax.sound.midi.Sequence midiSequence = MidiSystem.getSequence(inFile);
            MidiFileFormat midiFileFormat = MidiSystem.getMidiFileFormat(inFile);
            int type = midiFileFormat.getType();
            vavi.sound.mfi.Sequence mfiSequence = MfiSystem.toMfiSequence(midiSequence, type);

            File outFile = new File("sample.mld");
            MfiSystem.write(mfiSequence, VaviMfiFileFormat.FILE_TYPE, outFile);
```

### 3. Create ring tone w/ voice

```java
            int time = ...;
            InputStream waveStream = ...;

            Sequence sequence = new Sequence();
            Track track = sequence.createTrack();
            MfiMessage message;

            message = new CuePointMessage(0x00, 0x00);
            track.add(new MfiEvent(message, 0l));

            double aDelta = (60d / 120d) / 120d * 1000;
            int delta = (int) Math.round(time / aDelta);

            message = new TempoMessage(0x00, 0xcb, 0x78);
            track.add(new MfiEvent(message, 0l));

            MitsubishiMessage mdmessage = new MitsubishiMessage();
            mdmessage.toVolume(0x3f);
            track.add(new MfiEvent(mdmessage, 0l));

            mdmessage = new MitsubishiMessage();
            mdmessage.toPan(0x20);
            track.add(new MfiEvent(mdmessage, 0l));

            mdmessage = new MitsubishiMessage();
            mdmessage.toVoice(waveStream, 8000, 4, false);
            track.add(new MfiEvent(mdmessage, 0l));

            for (int i = 0; i < delta / 256; i++) {
                message = new NopMessage(0xff, 0);
                track.add(new MfiEvent(message, 0l));
            }
            message = new NopMessage(delta % 256, 0);
            track.add(new MfiEvent(message, 0l));

            message = new EndOfTrackMessage(0, 0);
            track.add(new MfiEvent(message, 0l));

            MfiSystem.write(sequence, VaviMfiFileFormat.FILE_TYPE, new File("output.mld"));
```
