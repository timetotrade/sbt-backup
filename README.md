# sbt-backup 
[![Build Status](https://travis-ci.org/Sensatus/sbt-backup.svg?branch=master)](https://travis-ci.org/Sensatus/sbt-backup) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.sensatus/sbt-backup/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.sensatus/sbt-backup)

This is a simple [SBT](http://www.scala-sbt.org) [AutoPlugin](http://www.scala-sbt.org/0.13/docs/Plugins.html)
that uses [scala-ssh](https://github.com/sirthias/scala-ssh) and [Apache Commons Compress]
(http://commons.apache.org/proper/commons-compress/) to compress and scp a provided directory to 
some remote host.

The plugin gives you the task ```backup``` which can be inserted into your build process as 
needed

It could be used to backup test results, documentation or any other supplementary material 
generated during packaging.


## Usage

Add to your project/plugins.sbt

```scala
addSbtPlugin("com.sensatus" % "sbt-backup" % "1.0.0")
```
                  
And enable in your project/Build.scala

```scala
.enablePlugins(SbtBackup)
```

The following keys are used for configuration:
    
 Key            | Type          |Description                                | Default value
----------------|---------------|-------------------------------------------|----------------------
backupHostname  | Option[String]| hostname to connect to                    | None
backupPort      | Int           | which port on remote host to connect to   | 22
backupKeyFile   | Option[File]  | key file to use to authenticate           | None
backupUsername  | String        | username to connect as                    | System.getProperty("user.name")
backupSourceDir | Option[File]  | which directory to compress and transfer  | None
backupRemoteDir | File          | remote directory in which to put the file | file(".")

if ```backupKeyFile``` is not provided, it will attempt to use any keys it can find in ~/.ssh/

```backupSourceDir``` is a taskKey rather than a settings key so that the 'input' to this task can
depend on the output of some other task. For example:

```scala
backupSourceDir := (unidoc in Compile).value.headOption
```

### Details

[scala-ssh](https://github.com/sirthias/scala-ssh) relies on [sshj](https://github
.com/shikhar/sshj) for the underlying transport, therefore this only supports the protocols that are
[supported by sshj](https://github.com/shikhar/sshj#supported-algorithms)

Currently only tar.gz compression is used and the use of ssh-key based authentication is hard coded

