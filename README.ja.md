Android USB MIDI ドライバ
====

Android USBホストAPIを使った、USB MIDIのドライバです。

- root不要
- 標準的なUSB MIDIデバイス(シーケンサや楽器など)をサポート
- プロトコルがUSB MIDIな、非標準なUSB MIDI機器をサポート
 - YAMAHA, Roland, MOTUのデバイスが接続できます(が、充分にテストされていません)

制限
----
- 一度に1つのデバイスしか接続できません。

必要なもの
----
- Android : OSバージョン3.1以降(API Level 11)で、USBホストのポートがあること。
- USB MIDI(互換な)デバイス

デバイスの接続
----
```
Android [USB Aポート]------[USB Bポート] USB MIDI デバイス
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

- ライブラリプロジェクトをcloneします。
- Eclipseのワークスペースにライブラリプロジェクトをインポートし、ビルドします。
- 新しいAndroid Projectを作成し、プロジェクトのライブラリにライブラリプロジェクトを 追加します。
- `jp.kshoji.driver.midi.activity.AbstractMidiActivity` をオーバーライドしたActivityを作ります。
- AndroidManifest.xml ファイルの activity タグを変更します。
 - **intent-filter** android.hardware.usb.action.USB_DEVICE_ATTACHED と **meta-data** を オーバーライドした Activity に対して追加します。
 - Activityの **launchMode** を "singleTask" にします。

```xml
        <activity
            android:name=".MyMidiMainActivity"
            android:label="@string/app_name"
            android:launchMode="singleTask" >
            <intent-filter>
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
            </intent-filter>
    
            <meta-data
                android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
                android:resource="@xml/device_filter" />
        </activity>
```

MIDI イベントの受信

- MIDIイベントを処理するメソッド (`"onMidi..."` という名前)を実装します。

MIDI イベントの送信

- `MIDIOutputDevice`のインスタンスを取得するために、AbstractMidiActivity の`getMidiOutputDevice()`メソッドを呼びます。
 - そのインスタンスの`"onMidi..."` という名前のメソッドを呼ぶとMIDIイベントが送信できます。

Mavenで使う
----
maven-android-pluginを使うには、Maven 3.0.4以降が必要です。
AndroidアプリをMavenを使ってビルドする方法について、詳しくは「maven-android-plugin」プロジェクトのwikiを参照してください。 http://code.google.com/p/maven-android-plugin/wiki/GettingStarted

- Eclipseから新規Mavenプロジェクトを作成します。
- 「Android 3.1」の依存性をmavenからインストールします。こちらのツールを使ってください。 https://github.com/mosabua/maven-android-sdk-deployer
- 作成したプロジェクトの `pom.xml` ファイルを以下のように編集します。(サンプルプロジェクトの `pom.xml` も参考にしてみてください)。

```xml
    <repositories>
        <repository>
            <id>midi-driver-snapshots</id>
            <url>http://github.com/kshoji/USB-MIDI-Driver/raw/master/snapshots</url>
        </repository>
    </repositories>
    
    <dependencies>
        <dependency>
            <groupId>jp.kshoji</groupId>
            <artifactId>midi-driver</artifactId>
            <version>${project.version}</version>
            <type>apklib</type>
        </dependency>
        
        <dependency>
            <groupId>android</groupId>
            <artifactId>android</artifactId>
            <version>3.1_r3</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

ライセンス
----
[Apache License, Version 2.0][Apache]
[Apache]: http://www.apache.org/licenses/LICENSE-2.0
