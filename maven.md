Use library with Maven
====

Maven 3.0.4 or later required for maven-android-plugin.
More infomation to build Android application with Maven, See the 'maven-android-plugin' project's wiki. http://code.google.com/p/maven-android-plugin/wiki/GettingStarted

- Create new Maven project with Eclipse
- Install "Android 3.1" dependency with maven, using this tool: https://github.com/mosabua/maven-android-sdk-deployer
- Edit `pom.xml` file for the created project, like below. (See also Sample Project's `pom.xml` file).

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
