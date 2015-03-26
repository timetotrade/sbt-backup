# sbt-scp-backup [![Build Status](https://travis-ci.org/Sensatus/sbt-scp-backup.svg?branch=master)](https://travis-ci.org/Sensatus/sbt-scp-backup)

This is a simple [SBT](http://www.scala-sbt.org) [AutoPlugin](http://www.scala-sbt.org/0.13/docs/Plugins.html)
that uses [scala-ssh](https://github.com/sirthias/scala-ssh) and [Apache Commons Compress]
(http://commons.apache.org/proper/commons-compress/) to compress and scp a provided directory to 
some remote host.

The plugin gives you the task ```scpBackup``` which can be inserted into your build process as 
needed

It could be used to backup test results, documentation or any other supplementary material 
generated during packaging.


## Usage

Add to your project/plugins.sbt

```scala
addSbtPlugin("com.sensatus" % "sbt-scp-backup" % "1.0.0")
```

The following keys are used for configuration:
    
 Key         | Type          |Description                                | Default value
-------------|---------------|-------------------------------------------|----------------------
scpHostname  | Option[String]| hostname to connect to                    | None
scpPort      | Int           | which port on remote host to connect to   | 22
scpKeyFile   | Option[File]  | key file to use to authenticate           | None
scpUsername  | String        | username to connect as                    | System.getProperty("user.name")
scpSourceDir | Option[File]  | which directory to compress and transfer  | None
scpRemoteDir | File          | remote directory in which to put the file | file(".")

if ```scpKeyFile``` is not provided, the default locations of ~/.ssh/id_rsa and ~/.ssh/id_dsa 
will be tried.

```scpSourceDir``` is a taskKey rather than a settings key so that the 'input' to this task can 
depend on the output of some other task. For example:

```scala
scpSourceDir := (unidoc in Compile).value.head
```

### Details

[scala-ssh](https://github.com/sirthias/scala-ssh) relies on [sshj](https://github
.com/shikhar/sshj) for the underlying transport, therefore this only supports the protocols that are
[supported by sshj](https://github.com/shikhar/sshj#supported-algorithms)

Currently only tar.gz compression is used and the use of ssh-key based authentication is hard coded

