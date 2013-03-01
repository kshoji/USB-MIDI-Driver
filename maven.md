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

Update/Release process with Maven
====

Currently, the processes do not work on github.
This section has written for developing with local git repository.
The local git repository can create with `git init --bare` command.

Update the snapshot
-------------------
Execute this on the command line.

```
mvn -DaltDeploymentRepository=snapshots::default::file:snapshots clean deploy
```

Then, xml and apklib files are generated in `snapshots` directory.
Commit these and push to the repository.

Release a new version
---------------------
At first, you must edit `pom.xml` file's `<scm>` section.

```xml
<scm>
	<url>file:///path/to/repo</url>
    <connection>scm:git:file:///path/to/repo</connection>
	<developerConnection>scm:git:file:///path/to/repo</developerConnection>
</scm>
```

Then execute this on the command line.

```
mvn release:clean release:prepare
```

The packages will be uploaded to github. And the new tag will be added.
And now, execute this for release completion.

```
mvn release:perform
```
