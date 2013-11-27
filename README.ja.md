Android USB MIDI ドライバ
====

Android USBホストAPIを使った、USB MIDIのドライバです。

- root不要
- 標準的なUSB MIDIデバイス(シーケンサや楽器など)をサポート
- プロトコルがUSB MIDIな、非標準なUSB MIDI機器をサポート
    - YAMAHA, Roland, MOTUのデバイスが接続できます(が、充分にテストされていません)。
- 複数のデバイスを接続できます。
- `javax.sound.midi` 互換のクラスをサポート
    - 詳細は [javax.sound.midi ドキュメント(英語)](https://github.com/kshoji/USB-MIDI-Driver/wiki/javax.sound.midi-porting-for-Android) に記載があります。

必要なもの
----
- Android : OSバージョン3.1以降(API Level 12)で、USBホストのポートがあること。
- USB MIDI(互換な)デバイス

オプション

- セルフパワーのUSBハブ(複数のUSB MIDIデバイスを接続したい場合)
- USB OTGケーブル(Android端末がUSB Aポートを持っていない場合)
- USB MIDI←→レガシーMIDI(MIDI 1.0)変換ケーブル(レガシーMIDIの楽器と接続したい場合)

デバイスの接続
----

一つのデバイスの場合
```
Android [USB Aポート / USB OTGケーブル]--- USB MIDI デバイス
```

複数のデバイスの場合
```
Android [USB Aポート / USB OTGケーブル]---(USBハブ) --┬--[USB Bポート] USB MIDI デバイス
                                                   ├-- [USB Bポート] USB MIDI デバイス
                                                   └   ...
```

リポジトリの概要
==============

- ライブラリプロジェクト : `MIDIDriver`
    - USB MIDIデバイスを接続するためのドライバ

- サンプルプロジェクト : `MIDIDriverSample`
    - ライブラリを使って実装した、シンセサイザ・MIDIイベントロガーの例
    - コンパイル済のサンプルプロジェクトを[Google Play Market](https://play.google.com/store/apps/details?id=jp.kshoji.driver.midi.sample)で公開しています。

ライブラリプロジェクトの使い方
==========================

プロジェクトの設定
----------------

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
<uses-feature android:name="android.hardware.usb.host" /> 

<application>
    <activity
        android:name=".MyMidiMainActivity"
        android:label="@string/app_name"
        android:launchMode="singleTask" >
        <intent-filter>
            <category android:name="android.intent.category.LAUNCHER" />
            <action android:name="android.intent.action.MAIN" />
        </intent-filter>
    </activity>
    :
```

AbstractSingleMidiActivity を用いた MIDI イベント処理
--------------------------------------------------

MIDI イベントの受信

- MIDIイベントを処理するメソッド (`"onMidi..."` という名前)を実装します。
- 注意: `"onMidi..."` メソッドは(UIスレッドではない)別のスレッドから呼ばれるので、UIスレッド内の`Handler`と`Callback`を用いてビューを操作する必要があります。下のコードのような感じです。

<a name="ui_thread"></a>
```java
public class SampleActivity extends AbstractSingleMidiActivity {

    // this field belongs to the UI thread
    final Handler uiThreadEventHandler = new Handler(new Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if ("note on".equals(msg.obj)) {
                textView.setText("note on event received.");
            }

            // message handled successfully
            return true;
        }
    });

    // this method will be called from the another thread, so it can't change View's state.
    @Override
    public void onMidiNoteOn(final MidiInputDevice sender, int cable, int channel, int note, int velocity) {
        // Send a message to the UI thread
        String message = "note on";
        uiThreadEventHandler.sendMessage(Message.obtain(uiThreadEventHandler, 0, message));
    }
```

MIDI イベントの送信 TODO

- AbstractSingleMidiActivity の`getMidiOutputDevices()`メソッドを呼び出すと、`MIDIOutputDevice`のインスタンスを取得できます。
    - そのインスタンスの`"sendMidi..."` という名前のメソッドを呼ぶとMIDIイベントが送信できます。
        - 接続されている、最初に見つかったUSB MIDIデバイスを取得します。
        - USB MIDIデバイスが接続されていない場合、メソッド`getMidiOutputDevices()`はnullを返します。


AbstractMultipleMidiActivity を用いた MIDI イベント処理
----------------------------------------------------

MIDI イベントの受信

- MIDIイベントを処理するメソッド (`"onMidi..."` という名前)を実装します。
    - イベントを送信したデバイスの情報(`MIDIInputDevice`のインスタンス)が最初の引数に設定されて呼ばれます。
- 注意: `"onMidi..."` メソッドは(UIスレッドではない)別のスレッドから呼ばれるので、UIスレッド内の`Handler`と`Callback`を用いてビューを操作する必要があります。[前述のコード](#ui_thread)のような感じです。

MIDI イベントの送信

- `Set<MIDIOutputDevice>`のインスタンスを取得するために、AbstractMidiActivity の`getMidiOutputDevices()`メソッドを呼びます。
- 接続したいMIDIOutputDeviceインスタンスを`Set<MIDIOutputDevice>`から選びます。
    - そのインスタンスの`"sendMidi..."` という名前のメソッドを呼ぶとMIDIイベントが送信できます。


サポートしているMIDIイベントの一覧
----

いくつかの種類のメッセージはそんなに頻繁に送信されません。なので、そのような頻度の低いメッセージあまりテストされていません。
もしトラブルなどありましたら [issue](https://github.com/kshoji/USB-MIDI-Driver/issues/new) に追加してください。

| メソッド名の終端                  | 意味                                                               | 充分なテストがされている?  |
|:----                            |:----                                                              |:----                    |
| MidiMiscellaneousFunctionCodes  | その他の機能。将来の拡張のために予約されています。                       | NO                      |
| MidiCableEvents                 | Cableに対するイベント。将来の拡張のために予約されています。               | NO                      |
| MidiSystemCommonMessage         | システム共通のメッセージ、もしくは1バイトから成るSysEx                   | NO                      |
| MidiSystemExclusive             | SysEx                                                             | YES                     |
| MidiNoteOff                     | ノート・オフ                                                        | YES                     |
| MidiNoteOn                      | ノート・オン                                                        | YES                     |
| MidiPolyphonicAftertouch        | ノート単位のアフタータッチ                                            | NO                      |
| MidiControlChange               | コントロール・チェンジ                                                | YES                     |
| MidiProgramChange               | 音色の変更                                                          | YES                     |
| MidiChannelAftertouch           | チャンネル単位のアフタータッチ                                         | NO                      |
| MidiPitchWheel                  | ピッチベンド                                                        | YES                     |
| MidiSingleByte                  | 1バイトのメッセージ                                                  | NO                      |


FAQ
----
- メソッド`"onMidi..."` や `"sendMidi..."`の引数 'cable' とはなんですか。
    - 一つのUSB MIDIデバイスには複数の仮想MIDIケーブルを持つことができます。 
    これはMIDIチャンネルを拡張するのに使われています。cable番号は、0から15が指定できます。
- USBデバイスを接続したのに、アプリがデバイスを認識しません。
    - [Trouble shooting(英語)](https://github.com/kshoji/USB-MIDI-Driver/wiki/TroubleShooting-on-connecting-an-USB-MIDI-device) を参考にしてみてください。

ライセンス
----
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
