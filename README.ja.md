Android USB MIDI ドライバ
====

Android USBホストAPIを使った、USB MIDIのドライバです。

- root不要
- 標準的なUSB MIDIデバイス(シーケンサや楽器など)をサポート
- プロトコルがUSB MIDIな、非標準なUSB MIDI機器をサポート
    - YAMAHA, Roland, MOTUのデバイスが接続できます(が、充分にテストされていません)。
- 複数のデバイスを接続できます。
- `javax.sound.midi` 互換のクラスをサポート
    - 詳細は [javax.sound.midi ドキュメント](javax.sound.midi.md) に記載があります。

必要なもの
----
- Android : OSバージョン3.1以降(API Level 12)で、USBホストのポートがあること。
- USB MIDI(互換な)デバイス

デバイスの接続
----
```
Android [USB Aポート]---(USBハブ)---[USB Bポート] USB MIDI デバイス
                               ---[USB Bポート] USB MIDI デバイス
                               ...
```

プロジェクト
----
- ライブラリ  
 - MIDIDriver : 本ライブラリ。

- サンプル
 - MIDIDriverSample : ライブラリを使って実装した、シンセサイザ・MIDIイベントロガーの例

ライブラリプロジェクトの使い方
----
プロジェクトの設定

- ライブラリプロジェクトをgit cloneします。
- Eclipseのワークスペースにライブラリプロジェクトをインポートし、ビルドします。
- 新しいAndroid Projectを作成し、プロジェクトのライブラリにライブラリプロジェクトを 追加します。
- `jp.kshoji.driver.midi.activity.AbstractMidiActivity` をオーバーライドしたActivityを作ります。
- `AbstractSingleMidiActivity` もしくは `AbstractMultipleMidiActivity` をオーバーライドしたActivityを作ります。
    - `AbstractSingleMidiActivity` は **一つだけ** MIDIデバイスを接続できます.
    - `AbstractMultipleMidiActivity` は **複数の** MIDIデバイスを接続できます.
        - メモ：たくさんのデバイスを接続すると、パフォーマンスが下がることがあります(具体的にはレイテンシが大きくなったり、高いCPU使用率になる)。
- AndroidManifest.xml ファイルの activity タグを変更します。
    - Activityの **launchMode** を "singleTask" にします。

```xml
        <activity
            android:name=".MyMidiMainActivity"
            android:label="@string/app_name"
            android:launchMode="singleTask" >
            <intent-filter>
                <category android:name="android.intent.category.LAUNCHER" />
                <action android:name="android.intent.action.MAIN" />
            </intent-filter>
        </activity>
```

MIDI イベントの受信

- MIDIイベントを処理するメソッド (`"onMidi..."` という名前)を実装します。

MIDI イベントの送信

- `Set<MIDIOutputDevice>`のインスタンスを取得するために、AbstractMidiActivity の`getMidiOutputDevices()`メソッドを呼びます。
- 接続したいMIDIOutputDeviceインスタンスを`Set<MIDIOutputDevice>`から選びます。
    - そのインスタンスの`"sendMidi..."` という名前のメソッドを呼ぶとMIDIイベントが送信できます。


ライセンス
----
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
