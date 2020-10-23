/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.io.IOException;
import java.util.Properties;

import vavi.util.Debug;


/**
 * Constants for Midi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020703 nsano initial version <br>
 */
public final class MidiConstants {

    /** */
    private MidiConstants() {
    }

    /** */
    private static Properties props = new Properties();

    /** */
    public static String getInstrumentName(int index) {
        return props.getProperty("midi.inst.gm." + index);
    }

    //-------------------------------------------------------------------------

    // メタ・イベント
    //
    // ここでは、いくつかのメタ・イベントについて定義がなされる。すべてのプロ
    // グラムがすべてのメタ・イベントをサポートしなければならないということで
    // はない。最初に定義されたメタ・イベントには以下のものがある。

    /**
     * <pre>
     * FF 00 02 ssss  [シーケンス・ナンバー]
     * </pre>
     * このオプション・イベントは、トラックの冒頭、任意の 0 でないデルタ・タイ
     * ムの前で、かつ任意の転送可能な MIDI イベントの前に置かれなければならず
     * 、これがシーケンスのナンバーを特定する。このトラック中のナンバーは
     * 1987 年夏の MMA ミーティングで協議された新 Cue メッセージの中のシーケンス
     * ・ナンバーに対応する。これは、フォーマット 2 の MIDI ファイルにおいては
     * 、各々の「パターン」を識別することによって、Cue メッセージを用いている
     * 「ソング」シーケンスがパターンを参照できるようにするために用いられる。
     * ID ナンバーが省略されているときは、そのシーケンスのファイル中での位置
     * [訳注：先頭から何番目のシーケンスか]がデフォールトとして用いられる。
     * フォーマット 0 または 1 の MIDI ファイルにおいては、１つのシーケンスしか
     * 含まれないため、このナンバーは第 1 の(つまり唯一の)トラックに含まれ
     * ることになる。いくつかのマルチトラック・シーケンスの転送が必要な場合は
     * 、各々が異なるシーケンス・ナンバーを持つフォーマット１のファイルのグル
     * ープとして行われなければならない。
     */
    public static final int META_SEQUENCE_NO = 0x00;

    /**
     * <pre>
     * FF 01 len text  [テキスト・イベント]
     * </pre>
     * 任意の大きさおよび内容のテキスト。トラックのいちばん初めに、トラック名
     * 、意図するオーケストレイション、その他ユーザがそこに置きたいと思う情報
     * を書いておくと良い。テキスト・イベントは、トラック中でその他の時に入れ
     * て歌詞やキュー・ポイントの記述として用いることもできる。このイベント中
     * のテキストは、最大限の互換性を確保するために、印刷可能なアスキー・キャ
     * ラクタでなければならない。しかし、高位ビットを用いる他のキャラクタ・コ
     * ード(訳注：漢字コードのような 2 バイト・コードなど)も、拡張されたキャ
     * ラクタ・セットをサポートする同じコンピュータ上の異なるプログラム間でフ
     * ァイルを交換するために用いることができる。非アスキー・キャラクタをサポ
     * ートしない機種上のプログラムは、このようなコードを無視しなければならな
     * い。
     *
     * (0.06 での追加事項） メタ・イベントのタイプ 01 から 0F までは様々なタ
     * イプのテキスト・イベントのために予約されている。この各々は上記のテキス
     * ト・イベントの特性と重複しているが、以下のように、異なる目的のために用
     * いられる。
     */
    public static final int META_TEXT_EVENT = 0x01;

    /**
     * <pre>
     * FF 02 len text  [著作権表示]
     * </pre>
     * 著作権表示を、印刷可能なアスキー・テキストとして持つ。この表示には(C)
     * の文字と、著作物発行年と、著作権所有者名とが含まれなければならない。ひ
     * とつの MIDI ファイルに幾つかの楽曲がある時には、すべての著作権表示をこ
     * のイベントに置いて、それがファイルの先頭に来るようにしなければならない
     * 。このイベントは第１トラック・ブロックの最初のイベントとして、デルタ・
     * タイム = 0 で置かれなければならない。
     */
    public static final int META_COPYRIGHT = 0x02;

    /**
     * <pre>
     * FF 03 len text  [シーケンス名またはトラック名]
     * </pre>
     * フォーマット 0 のトラック、もしくはフォーマット 1 のファイルの第 1 トラッ
     * クにおいては、シーケンスの名称。その他の場合は、トラックの名称。
     */
    public static final int META_NAME = 0x03;

    /**
     * <pre>
     * FF 04 len text  [楽器名]
     * </pre>
     * そのトラックで用いられるべき楽器編成の種類を記述する。 MIDI の冒頭に置
     * かれるメタ・イベントとともに用いて、どの MIDI チャネルにその記述が適用
     * されるかを特定することもある。あるいは、チャネルをこのイベント中のテキ
     * ストで特定しても良い。
     */
    public static final int META_INSTRUMENT = 0x04;

    /**
     * <pre>
     * FF 05 len text  [歌詞]
     * </pre>
     * 歌詞。一般的には、各音節がそのイベントのタイムから始まる独立した歌詞イ
     * ベントとなる。
     */
    public static final int META_LYRICS = 0x05;

    /**
     * <pre>
     * FF 06 len text  [マーカー]
     * </pre>
     * 通常フォーマット 0 のトラック、もしくはフォーマット 1 のファイルの第１ト
     * ラックにある。リハーサル記号やセクション名のような、シーケンスのその時
     * 点の名称。(「First Verse」等)
     */
    public static final int META_MARKER = 0x06;

    /**
     * <pre>
     * FF 07 len text  [キュー・ポイント]
     * </pre>
     * スコアのその位置において、フィルム、ヴィデオ・スクリーン、あるいはステ
     * ージ上で起こっていることの記述。(「車が家に突っ込む」「幕が開く」「女
     * は男に平手打ちを食わせる」等)
     */
    public static final int META_QUE_POINT = 0x07;

    /**
     * <pre>
     * FF 2F 00  [トラックの終わり]
     * </pre>
     * このイベントは省略することができない。これがあることによってトラックの
     * 正しい終結点が明確になり、トラックが正確な長さを持つようになる。これは
     * トラックがループになっていたり連結されていたりする時に必要である。
     */
    public static final int META_END_OF_TRACK = 0x2f;

    /**
     * <pre>
     * FF 51 03 tttttt  [テンポ設定(単位は μsec / MIDI 四分音符)]
     * </pre>
     * このイベントはテンポ・チェンジを指示する。「μsec / MIDI 四分音符」は
     * 言い換えれば「(μsec / MIDI クロック)の 24 分の 1」である。テンポを
     * 「拍 / 時間」ではなく「時間 / 拍」によって与えることで、 SMPTE タイム・
     * コードや MIDI タイム・コードのような実時間ベースの同期プロトコルを用い
     * て、絶対的に正確な長時間同期を得ることができる。このテンポ設定で得られ
     * る正確さは、120 拍 / 分で 4 分の曲を終わった時に誤差が 500 μsec 以内
     * にとどまる、というものである。理想的には、これらのイベントは Cue の
     * 上で MIDI クロックがある位置にのみ、おかれるべきである。このことは、ほか
     * の同期デバイスとの互換性を保証しよう、少なくとも、その見込みを増やそう
     * 、というもので、この結果、この形式で保存された拍子記号やテンポ・マップ
     * は容易にほかのデバイスへ転送できることになる。
     */
    public static final int META_TEMPO = 0x51;

    /**
     * <pre>
     * FF 54 05 hr mn se fr ff  [SMPTE オフセット(0.06 での追加 - SMPTE フォーマットの記述)]
     * </pre>
     * このイベントは、もしあれば、トラック・ブロックがスタートすることになっ
     * ている SMPTE タイムを示す。これは、トラックの冒頭に置かれなければなら
     * ない。すなわち、任意の 0 でないデルタ・タイムの前で、かつ任意の転送可能
     * な MIDI イベントの前である。時間は、 MIDI タイム・コードと全く同様に
     * SMPTE フォーマットでエンコードされなければならない。フォーマット 1 のフ
     * ァイルにおいては、 SMPTE オフセットはテンポ・マップとともにストアされ
     * る必要があり、他のトラックにあっては意味をなさない。デルタ・タイムのた
     * めに異なるフレーム分解能を指定している SMPTE ベースのトラックにおいて
     * も、ff のフィールドは細分化されたフレーム(100 分の 1 フレーム単位)を
     * 持っている。
     */
    public static final int META_SMPTE_OFFSERT = 0x54;

    /**
     * <pre>
     * FF 58 04 nn dd cc bb  [拍子記号]
     * </pre>
     * 拍子記号は、4 つの数字で表現される。nn と dd は、記譜する時のように、拍子
     * 記号の分子と分母を表す。分母は 2 のマイナス乗である。すなわち、2 は四分
     * 音符を表し、3 は八分音符を表す、等々。パラメータ cc は、1 メトロノーム・
     * クリックあたりの MIDI クロック数を表現している。パラメータ bb は、 MIDI
     * 四分音符(24 MIDI クロック)の中に記譜上の三十二分音符がいくつ入るかを
     * 表現している。このパラメータは、 MIDI 上の四分音符(24 クロック)を他
     * のものとして記譜し、あるいは表現上他の音符に対応させるようユーザ定義で
     * きるプログラムが既に数多く存在することから加えられた。
     *
     * 従って、6 / 8 拍子で、メトロノームは八分音符 3 つ毎に刻むけれども四分音
     * 符 24 クロックで、1 小節あたりでは 72 クロックになる拍子は、16 進で次
     * のようになる。
     *
     *   FF 58 04 06 03 24 08
     *
     * これは、8 分の 6 拍子で(8 は 2 の 3 乗なので、06 03となる)、付点四分音
     * 符あたり 36 MIDI クロック(16進で24！)[*2]で、 MIDI 四分音符に記
     * 譜上の三十二分音符が 8 つ対応するということを示している。
     */
    public static final int META_58 = 0x58;

    /**
     * <pre>
     * FF 59 02 sf mi  [調号]
     *   sf = -7  フラット７つ
     *   sf = -1  フラット１つ
     *   sf = 0   ハ調
     *   sf = 1   シャープ１つ
     *   sf = 7   シャープ７つ
     *
     *   mi = 0   長調
     *   mi = 1   短調
     * </pre>
     */
    public static final int META_59 = 0x59;

    /**
     * <pre>
     * FF 7F len data  [シーケンサー特定メタ・イベント]
     * </pre>
     * 特定のシーケンサーのための特別な要求にこのイベント・タイプを用いること
     * ができる。データ・バイトの最初の１バイトはメーカーIDである。しかしな
     * がら、これは交換用フォーマットなのであるから、このイベント・タイプの使
     * 用よりもスペック本体の拡張の方が望ましい。このタイプのイベントは、これ
     * を唯一のファイル・フォーマットとして用いることを選択したシーケンサーに
     * よって使用されるかもしれない。仕様詳細のフォーマットが確定したシーケン
     * サーにおいては、このフォーマットを用いるにあたって標準仕様を守るべきで
     * あろう。
     */
    public static final int META_MACHINE_DEPEND = 0x7f;

    //-------------------------------------------------------------------------

    /**
     * 01H〜1FH
     * 00H 00H 01H〜00H 1FH 7FH
     */
    public static final int SYSEX_MAKER_ID_American = 0x01;

    /**
     * 20H〜3FH
     * 00H 20H 00H〜00H 3FH 7FH
     */
    public static final int SYSEX_MAKER_ID_European = 0x20;

    /**
     * 40H〜5FH
     * 00H 40H 00H〜00H 5FH 7FH
     */
    public static final int SYSEX_MAKER_ID_Japanese = 0x40;

    /**
     * 60H〜7CH
     * 00H 60H 00H〜00H 7FH 7FH
     */
    public static final int SYSEX_MAKER_ID_Other = 0x60;

    /**
     * 7DH〜7FH
     */
    public static final int SYSEX_MAKER_ID_Special = 0x70;

    // 7DH
    // メーカー ID の 7DH は、学校教育機関などでの研究用に使用される ID で、
    // 非営利目的でのみ使用できます。
    //
    // 7EH
    // 7EH はノンリアルタイムユニバーサルシステムエクスクルーシブです。
    // エクスクルーシブのうち、メーカーや機種を超えてやり取りできると
    // 便利なデータ転送に使用されます。なお、7EHは実時間に関係のないデータ転送に使用されます。
    //
    // 7FH
    // 7FH はリアルタイムユニバーサルシステムエクスクルーシブです。
    // 7EH のノンリアルタイムユニバーサルシステムエクスクルーシブと同様に、
    // メーカーや機種を超えてデータをやり取りするときに使用され、
    // 7FHでは実時間に関係のあるデータ転送に利用されます。

    //-------------------------------------------------------------------------

    /** */
    static {
        try {
            final Class<?> clazz = MidiConstants.class;
            props.load(clazz.getResourceAsStream("midi.properties"));
        } catch (IOException e) {
Debug.println(e);
            throw new IllegalStateException(e);
        }
    }
}

/* */
