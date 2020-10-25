MFi のサービスプロバイダ実装例のクラスを提供します．

 * 特別な仕様として拡張情報の空番 0xfd を MetaMessage として使用します。

## TODO

 * ~~020630 VaviSequencer: stop()~~
 * ~~020706 VaviSequence: Midi Converter~~
 * ~~030826 MidiFileReader を作る~~
 * ~~030917 InfoMessage のいくつかは MetaMessage (1,2,3...) に変換可能~~
 * ~~041223 TempoMessage もデルタタイム対応させなければいけない~~
 * ~~041231 SMF type 1 対応 → 4ch を一つの ArrayList に入れてから処理とか~~
 * MFiConvertible は違うところで実装すべき？
 * ADPCM も Δ タイムにのっとって演奏する -> Thread 化？
 * SMF -> MFi がまだおかしい M$ 系のやつとか
 * ~~070117 { Normal, Class A, B, C, Note }, { Extension A, B, Info } の統合~~
 * Track, AudioData, Info* の統合
