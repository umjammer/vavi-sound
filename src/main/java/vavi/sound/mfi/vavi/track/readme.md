# vavi.sound.mfi.vavi.system

SysexMessage の派生クラスを提供します．

### Δタイム

  * テンポ ... 1 分間の 4 分音符の数
  * タイムベース ... 4 分音符の分解能 = PPQ (Parts ...), TpQN (Tick par Quarter Note)
```
        1Δ = 60[s] / テンポ / タイムベース;
```

## TODO

 * Δ > 255 の Note の取り扱い

 たとえば音調 380 (voice = 1)

|delta|voice|gateTime|説明|
|:-:|:-:|:-:|:-:|
|-|1|2 + 10 + 1 = 13||
|2|2|-||
|10|3|-||
|0|1|max(380 - 13, 255) = 255|voice が 1 ラウンドしたあとに？|

 PsmPlayer は飛ばした分を最初のΔから引いている

 * MFi 5.0 拡張用情報
