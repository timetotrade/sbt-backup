# sbt-scp-backup [![Build Status](https://travis-ci.org/Sensatus/sbt-scp-backup.svg?branch=master)](https://travis-ci.org/Sensatus/sbt-scp-backup)

This is a simple [SBT](http://www.scala-sbt.org) [AutoPlugin](http://www.scala-sbt.org/0.13/docs/Plugins.html)
that uses [scala-ssh](https://github.com/sirthias/scala-ssh) and [Apache Commons Compress]
(http://commons.apache.org/proper/commons-compress/) to compress and scp a provided directory to 
some remote host.

It could be used to backup test results, documentation or any other supplementary material 
generated during packaging.


## Usage

Add to your project/plugins.sbt

```scala
addSbtPlugin("com.sensatus" % "sbt-scp-backup" % "1.0.0")
```

And enable in your project/Build.scala

```scala
.enablePlugins(SbtScpBackup)
```

The following keys must be provided:
    
 Key         | Type   |Description                                | Default value
-------------|--------|-------------------------------------------|----------------------
scpHostname  | String | hostname to connect to                    |
scpPort      | Int    | which port on remote host to connect to   | 22
scpKeyFile   | File   | key file to use to authenticate           | file("~/.ssh/id_rsa")
scpUsername  | String | username to connect as                    | System.getProperty("user.name")
scpSourceDir | File   | which directory to compress and transfer  | 
scpRemoteDir | File   | remote directory in which to put the file | file(".")


### Details

It currently only supports tar.gz compression and will only use ssh-key based authentication
