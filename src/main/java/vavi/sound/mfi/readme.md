# vavi.sound.mfi

MFi サウンド関連のクラスを提供します．

## Abstract

{@link javax.sound.midi} パッケージとほぼ同じ構造をしています。
使用法は {@link javax.sound.midi} を参考にしてください。

{@link vavi.sound.mfi.MfiSystem.getSequencer()} で再生する場合は
システムプロパティ javax.sound.midi.Sequencer に "#Real Time Sequencer"
を明示するようにしてください。{@link vavi.sound.midi.VaviSequencer} が
デフォルトシーケンサになった場合 {@link vavi.sound.mfi.MfiSystem#getMetaEventListener()}
が重複して登録されてしまいます。

## TODO

 * 特殊命令の実装
 * ~~"030825" MfiFileFormat のヘッダ情報の扱いが未実装、メッセージにするか？ファイルフォーマットにするか？~~ →
 * ~~"030825" 出てない音階がある~~
 * MfiSystem converter の取得法が他と違う
 * ~~"090110">MfiSystem#main で鳴らない...~~

## License
オリジナルのソースは見あたらないため
一次ライセンスを気にする必要は無いと思われますが、
念のために記しておきます。

 * [open i-Melody Project](http://www.xucker.jpn.org/ood/java/imelody/")
 * [Academic Free License (AFL)](http://opensource.org/licenses/academic.php")
 * [GNU Library or Lesser General Public License (LGPL)](http://www.gnu.org/licenses/lgpl.html)
