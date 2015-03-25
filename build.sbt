/*
 Copyright 2015 Sensatus UK Ltd

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}
import ReleaseKeys._

lazy val root = (project in file("."))
  .settings(sonatypeSettings:_*)
  .settings(releaseSettings: _*)
  .settings(publishArtifactsAction := PgpKeys.publishSigned.value)
  .settings(
    sbtPlugin := true,
    name := "sbt-scp-backup",
    organization := "com.sensatus",
    organizationHomepage := Some(url("http://www.sensatus.com")),
    description := "SBT AutoPlugin to compress and scp a directory to a supplied server",
    startYear := Some(2015),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature"),
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-compress" % "1.9",
      "com.decodified" %% "scala-ssh" % "0.7.0",
      "org.bouncycastle" % "bcprov-jdk16" % "1.46",
      "com.jcraft" % "jzlib" % "1.1.3"
    ),
//    Publishing details:
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/sensatus/sbt-scp-backup"),
        "git@github.com:sensatus/sbt-scp-backup.git"
      )
    ),
    publishTo := {
      val sonatype = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at sonatype + "content/repositories/snapshots")
      else
        Some("releases"  at sonatype + "service/local/staging/deploy/maven2")
    },
    licenses := Seq("Apache-2.0" -> url("http://www.opensource.org/licenses/Apache-2.0")),
    homepage := Some(url("http://github.com/sensatus/sbt-scp-backup")),
    publishMavenStyle := true,
    developers := List(
      Developer("MaxWorgan","Max Worgan", "max.worgan@sensatus.com",url("http://www.sensatus.com"))
    ),
    // workaround for sbt/sbt#1834
    pomPostProcess := { (node: XmlNode) =>
      new RuleTransformer(new RewriteRule {
        override def transform(node: XmlNode): XmlNodeSeq = node match {
          case e: Elem
            if e.label == "developers" =>
            <developers>
              {developers.value.map { dev =>
              <developer>
                <id>{dev.id}</id>
                <name>{dev.name}</name>
                <email>{dev.email}</email>
                <url>{dev.url}</url>
              </developer>
            }}
            </developers>
          case _ => node
        }
      }).transform(node).head
    },
  )
