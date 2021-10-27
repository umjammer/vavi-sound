/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * ContentsInfo Chunk.
 * <pre>
 * "CNTI"
 *
 *  Contents Class ：1 byte (必須)
 *  Contents Type ：1 byte (必須)
 *  Contents Code Type ：1 byte (必須)
 *  Copy Status ：1 byte (必須)
 *  Copy Counts ：1 byte (必須)
 *  Option ：n byte (Option)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class ContentsInfoChunk extends Chunk {

    /** */
    public ContentsInfoChunk(byte[] id, int size) {
        super(id, size);
    }

    /** */
    public ContentsInfoChunk() {
        System.arraycopy("CNTI".getBytes(), 0, id, 0, 4);
        this.size = 5;
    }

    /** */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {

        this.contentsClass = read(is);
Debug.println("contentsClass: " + (contentsClass == 0 ? "YAMAHA" : "Vender ID(" + contentsClass + ")"));
        this.contentsType = read(is);
Debug.printf("contentsType: 0x02x\n", contentsType);
        this.contentsCodeType = read(is);
Debug.printf("contentsCodeType: 0x02x\n", contentsCodeType);
        this.copyStatus = read(is);
Debug.println("copyStatus: " + StringUtil.toBits(copyStatus, 8));
        this.copyCounts = read(is);
Debug.println("copyCounts: " + copyCounts);
        byte[] option = new byte[size - 5];
        read(is, option);
Debug.println("option: " + option.length + " bytes (subData)");
        int i = 0;
        while (i < option.length) {
//Debug.println(i + " / " + option.length + "\n" + StringUtil.getDump(option, i, option.length - i));
            SubData subDatum = new SubData(option, i, contentsCodeType);
            subData.put(subDatum.getTag(), subDatum);
Debug.println("ContentsInfo: subDatum: " + subDatum);
            i += 2 + 1 + subDatum.getData().length + 1; // tag ':' data ','
//Debug.println(i + " / " + option.length + "\n" + StringUtil.getDump(option, i, option.length - i));
        }
    }

    /** */
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        dos.writeByte(contentsClass);
        dos.writeByte(contentsType);
        dos.writeByte(contentsCodeType);
        dos.writeByte(copyStatus);
        dos.writeByte(copyCounts);
        for (SubData subDatum : subData.values()) {
            subDatum.writeTo(os);
        }
    }

    /** */
    public static final int CONTENT_CLASS_YAMAHA = 0x00;

    /** */
    private int contentsClass;

    /**
     * コンテンツのクラスを表現する。
     * ※PDA 端末等、将来の多機能端末で同様のデータフォーマットまで流用する場合の区別に用いる。
     */
    public void setContentsClass(int contentsClass) {
        this.contentsClass = contentsClass;
    }

    /** */
    private int contentsCodeType;

    /**
     * <pre>
     * 0x01 ISO 8859-1 (Latin-1) 英語、フランス語、ドイツ語 イタリア語、スペイン語、ポルトガル語
     * 0x02 EUC-KR(KS) 韓国語
     * 0x03 HZ-GB-2312 中国語(簡体字)
     * 0x04 Big5 中国語(繁体字)
     * 0x05 KOI8-R ロシア語など
     * 0x06 TCVN-5773:1993 ベトナム語
     * 0x07〜0x1F Reserved Reserved
     * 0x20 UCS-2 Unicode
     * 0x21 UCS-4 Unicode
     * 0x22 UTF-7 Unicode
     * 0x23 UTF-8 Unicode
     * 0x24 UTF-16 Unicode
     * 0x25 UTF-32 Unicode
     * 0x26〜0xFF Reserved Reserved
     * </pre>
     * ただし、Contents Info Chunk のOption においては、どのタイプも 『,(0x2C)』 はOption のデリミタとし
     * て定義しているため、エスケープキャラクタとしてのバックスラッシュ『\(0x5C)』 と合わせて使用する。
     * <pre>
     *  表記 機能
     *  \, “,” を表す
     *  \\ \を表す
     *  \ 無視する
     * </pre>
     * デリミタを正しく識別出来ないことが想定される文字コードが設定されている場合にはContents Info
     * Chunk のOption にデータを記述せず、Optional Data Chunk にデータを記述するものとする。
     */
    public void setContentsCodeType(int contentsCodeType) {
        this.contentsCodeType = contentsCodeType;
    }

    /** */
    private int contentsType;

    /**
     * コンテンツのタイプを表現する。
     * <pre>
     * 0x00〜0x0F, 0x30〜0x33 着信メロディ
     * 0x10〜0x1F, 0x40〜0x42 カラオケ系
     * 0x20〜0x2F, 0x50〜0x53 CM 系
     * 上記以外の未使用値 Reserved
     * </pre>
     */
    public void setContentsType(int contentsType) {
        this.contentsType = contentsType;
    }

    /** */
    private int copyCounts;

    /**
     * コピー回数を表現する。
     * <pre>
     * Copy Counts Description
     * 0x00〜0xFE Copy Counts
     * 0xFF Copy Counts(255 以上)
     * </pre>
     * ※ コピー／移動が発生するたびに１カウント進める。
     */
    public void setCopyCounts(int copyCounts) {
        this.copyCounts = copyCounts;
    }

    /** */
    private int copyStatus;

    /**
     * コンテンツの持つコピー定義を表現する。
     * <pre>
     *         | b7       | b6       | b5       | b4       | b3       | b2       | b1      |  b0
     * --------+----------+----------+----------+----------+----------+----------+---------+-----
     * 不可bit | Reserved | Reserved | Reserved | Reserved | Reserved | 編集     | 保存    | 転送
     * </pre>
     * b0 は転送の可/不可、b1 は保存の可/不可b2 は編集の可/不可を定義する。(0:可 1:不可)
     * Reserved bit は”1”で埋める。
     */
    public void setCopyStatus(int copyStatus) {
        this.copyStatus = copyStatus;
    }

    /** */
    private Map<String, SubData> subData = new TreeMap<>();

    /**
     * @return null when specified sub chunk is not found
     */
    public String getSubDataByTag(String tag) {
        SubData subDatum = subData.get(tag);
        if (subDatum == null) {
            return null;
        }
        try {
            return new String(subDatum.getData(), "Windows-31J"); // use contentsCodeType
        } catch (UnsupportedEncodingException e) {
            return new String(subDatum.getData());
        }
    }

    /**
     * Option (Contents Type 0x00, Contents Class 0x00 ~ 0xFF 用)
     * <p>
     * ジャンル名、曲名、アーティスト名、作詞/作曲者名等を格納する。必ずしも表示に使用するものではなく、個
     * 別データの認識に使用するものである。
     * データは可変長とし、『タグ (2byte)』+『 : (0x3A)』+『Data』+『 , (0x2C)』で区切って記述する。タグ名を以
     * 下とする。タグは2byte 固定のバイト列とする。
     * 『Data』内において『 , (0x2C)』を文字として使用する場合 のエスケープキャラクタを§4.2.3に定義する。
     * </p>
     * <pre>
     *  名称           | タグ名 | Hex
     * ----------------+--------+-----------
     *  ベンダー名     | VN     | 0x56 0x4E
     *  キャリア名     | CN     | 0x43 0x4E
     *  カテゴリー名   | CA     | 0x43 0x41
     *  曲名           | ST     | 0x53 0x54
     *  アーティスト名 | AN     | 0x41 0x4E
     *  作詞           | WW     | 0x57 0x57
     *  作曲           | SW     | 0x53 0x57
     *  編曲           | AW     | 0x41 0x57
     *  Copyright&copy;     | CR     | 0x43 0x52
     *  管理者団体名   | GR     | 0x47 0x52
     *  管理情報       | MI     | 0x4D 0x49
     *  作成日時       | CD     | 0x43 0x44
     *  更新日時       | UD     | 0x55 0x44
     * </pre>
     * Unicode の追加にあたり、Option 内のデリミタを識別できない文字コードが存在するため
     * Optional Data Chunk を追加した。
     */
    public void addSubData(String tag, String data) {
        SubData subDatum;
        try {
            subDatum = new SubData(tag, data.getBytes("Windows-31J")); // use contentsCodeType
        } catch (UnsupportedEncodingException e) {
            subDatum = new SubData(tag, data.getBytes());
        }
        subData.put(tag, subDatum);
        size += subDatum.getSize();
    }
}

/* */
