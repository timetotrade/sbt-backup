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
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{TrueFileFilter, RegexFileFilter, IOFileFilter}
import sbt.Keys._
import sbt.{File, _}
import sbt.plugins.JvmPlugin

import scala.collection.JavaConversions._
import scala.annotation.tailrec
import scala.util.control.Exception.nonFatalCatch

/**
 * Class which compresses a selected directory and scp's it to a remote host
 */
object SbtBackup extends AutoPlugin {

  override def requires: Plugins = JvmPlugin
  override def trigger = noTrigger

  object autoImport {
    lazy val backup          = taskKey[Unit]("Compress and scp a directory to configured host")
    lazy val backupUsername  = settingKey[String]("Username to scp with")
    lazy val backupKeyFile   = settingKey[Option[File]]("Keyfile to scp with")
    lazy val backupHostname  = settingKey[Option[String]]("Host to scp to")
    lazy val backupPort      = settingKey[Int]("Port to connect to")
    lazy val backupSourceDir = taskKey[Option[File]]("Directory to compress and transfer")
    lazy val backupRemoteDir = settingKey[File]("Remote directory to place tar.gz")
  }

  import com.sensatus.SbtBackup.autoImport._

  /**
   * Provide default values
   * @return
   */
  override def globalSettings = Seq(
    backupPort       := 22,
    backupRemoteDir  := file("."),
    backupUsername   := System.getProperty("user.name"),
    backupKeyFile    := None,
    backupHostname   := None,
    backupSourceDir  := None
  )

  private def expandPath(path:String):String = {
    path.replaceFirst("^~", System.getProperty("user.home"))
  }

  private def getAllPossibleKeys : List[String] = {
    FileUtils.listFiles(
      file(System.getProperty("user.home") + "/.ssh/"),
      new RegexFileFilter("id_.*[^\\.pub]"),
      TrueFileFilter.TRUE).toList.map(_.getPath)
  }
  /**
   * Set the project settings
   */
  override lazy val projectSettings = Seq(
    backup := {

      val log = streams.value.log
      val sdir = backupSourceDir.value
      val hname = backupHostname.value

      if(!sdir.isDefined)  log.error("backupSourceDir must be set to use backupBackup")
      if(!hname.isDefined) log.error("backupHostname must be set to use backupBackup")

      for { sDir ← sdir; sHost ← hname }{
        val keyFile = backupKeyFile.value.map(p ⇒ expandPath(p.getPath) :: Nil)
                      .getOrElse(getAllPossibleKeys)

        val compressedFile = sDir.getAbsolutePath + ".tar.gz"
        val hostConfig = HostConfig(
          PublicKeyLogin(backupUsername.value, keyFile: _*), sHost, backupPort.value,
          hostKeyVerifier = HostKeyVerifiers.DontVerify
        )

        val result = for {
          archive ← createArchive(sDir, compressedFile).right
          _       ← scpArchive(hostConfig, archive, backupRemoteDir.value.getPath).right
        } yield ()

        result.fold(
          s ⇒ log.error(s),
          _ ⇒ log.info("backupBackup completed")
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
   * backup the file to the remote host
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
      try{ addToArchive(tar, List(input → ".")) }
      finally{ tar.close(); fos.close() }
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
          try { IOUtils.copy(bis, t) }
          finally { t.closeArchiveEntry(); bis.close() }
          addToArchive(t, fs)
        } else if (f.isDirectory) {
          t.closeArchiveEntry()
          val children = f.listFiles().map(_ → (d + File.separator + f.getName)).toList
          addToArchive(t, children ++ fs)
        } else {
          t.closeArchiveEntry()
          addToArchive(t, fs)
        }
    }
  }
}