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

package com.sensatus

import java.io._

import com.decodified.scalassh._
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.utils.IOUtils
import sbt.Keys._
import sbt.{File, _}
import sbt.plugins.JvmPlugin

import scala.annotation.tailrec
import scala.util.control.Exception.nonFatalCatch

/**
 * Class which compresses a selected directory and scp's it to a remote host
 */
object SbtScpBackup extends AutoPlugin {

  override def requires: Plugins = JvmPlugin
  override def trigger = allRequirements

  object autoImport {
    lazy val scpBackup    = taskKey[Unit]("compress and scp a directory to configured host")
    lazy val scpUsername  = settingKey[String]("username to scp with")
    lazy val scpKeyFile   = settingKey[Option[File]]("keyfile to scp with")
    lazy val scpHostname  = settingKey[Option[String]]("host to scp to")
    lazy val scpPort      = settingKey[Int]("port to connect to")
    lazy val scpSourceDir = taskKey[Option[File]]("Directory to compress and transfer")
    lazy val scpRemoteDir = settingKey[File]("Remote directory to place tar.gz")
  }

  import com.sensatus.SbtScpBackup.autoImport._

  /**
   * Provide default values
   * @return
   */
  override def globalSettings = Seq(
    scpPort       := 22,
    scpRemoteDir  := file("."),
    scpUsername   := System.getProperty("user.name"),
    scpKeyFile    := None,
    scpHostname   := None,
    scpSourceDir  := None
  )

  /**
   * Set the project settings
   */
  override lazy val projectSettings = Seq(
    scpBackup := {

      val log = streams.value.log
      val sdir = scpSourceDir.value
      val hname = scpHostname.value

      if(!sdir.isDefined)  log.error("scpSourceDir must be set to use scpBackup")
      if(!hname.isDefined) log.error("scpHostname must be set to use scpBackup")

      for { sDir ← sdir; sHost ← hname }{
        val keyFile = scpKeyFile.value.map(_.getPath :: Nil)
          .getOrElse(PublicKeyLogin.DefaultKeyLocations)
          .map(_.replaceFirst("^~", System.getProperty("user" + ".home")))

        val compressedFile = sDir.getAbsolutePath + ".tar.gz"
        val hostConfig = HostConfig(
          PublicKeyLogin(scpUsername.value, keyFile: _*), sHost, scpPort.value,
          hostKeyVerifier = HostKeyVerifiers.DontVerify
        )

        val result = for {
          archive ← createArchive(sDir, compressedFile).right
          _       ← scpArchive(hostConfig, archive, scpRemoteDir.value.getPath).right
        } yield ()

        result.fold(
          s ⇒ log.error(s),
          _ ⇒ log.info("scpBackup completed")
        )

        val tmpFile = file(compressedFile)
        if (tmpFile.exists()) {
          if (!tmpFile.delete())
            log.error(s"Could not delete ${tmpFile.getAbsolutePath}")
        }
      }
    }
  )

  /**
   * Scp the file to the remote host
   * @param hostConfig the [[HostConfig]] specifying the remote connection
   * @param local the local file to transfer
   * @param remote the remote directory
   * @return [[Either]] with a [[String]] error, or Unit
   */
  def scpArchive(hostConfig:HostConfig,local:File,remote:String): Validated[Unit] = {
    SSH(hostConfig.hostName, hostConfig)(_.upload(local.getAbsolutePath, remote))
  }

  /**
   * Create an tar.gz archive from a directory
   * @param input the input directory
   * @param output the output file
   * @return [[Either]] the [[File]] or a [[String]] error
   */
  def createArchive(input:File,output:String) : Either[String,File] = {
    (nonFatalCatch either {
      val fos = new FileOutputStream(output)
      val tar = new TarArchiveOutputStream(
        new GzipCompressorOutputStream(
          new BufferedOutputStream(fos)
        )
      )
      tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR)
      tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
      addToArchive(tar, List(input → "."))
      tar.close()
      fos.close()
      file(output)
    }).left.map(_.getMessage)
  }

  /**
   * Recursive method to add a directory tree to a [[TarArchiveOutputStream]]
   * @param t the [[TarArchiveOutputStream]] to add files to
   * @param files the files and their directories to add
   * @return The [[TarArchiveOutputStream]] with all the files added
   */
  @tailrec
  private def addToArchive(t: TarArchiveOutputStream, files: List[(File,String)]):
  TarArchiveOutputStream = {
    files match {
      case Nil ⇒ t
      case (f,d) :: fs ⇒
        t.putArchiveEntry(new TarArchiveEntry(f, d + File.separator + f.getName))
        if (f.isFile) {
          val bis = new BufferedInputStream(new FileInputStream(f))
          IOUtils.copy(bis, t)
          t.closeArchiveEntry()
          bis.close()
          addToArchive(t, fs)
        } else if (f.isDirectory) {
          t.closeArchiveEntry()
          val children = f.listFiles().map(_ → (d + File.separator + f.getName)).toList
          addToArchive(t, children ++ fs)
        }
        else {
          t.closeArchiveEntry()
          addToArchive(t, fs)
        }
    }
  }
}