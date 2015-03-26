package com.sensatus

import java.io.{BufferedInputStream, FileInputStream, InputStream, _}

import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveInputStream, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.io.filefilter.TrueFileFilter
import org.scalatest.{Matchers, WordSpec}

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

class CompressionTests extends WordSpec with Matchers {

  "sbt-scp-backup compressor" must {

    "retain structural integrity of archives" in {
      val testDir = new File(getClass.getResource("/testdir").getPath)
      val outFile = System.getProperty("java.io.tmpdir") + "/test.tar.gz"
      val tarFile = SbtScpBackup.createArchive(testDir, outFile).left.map(e ⇒ fail(e)).right.get

      // strip trailing "/"s
      val files = Unpack(tarFile.getAbsolutePath).map(_.getName)
        .map(s ⇒ if(s.endsWith("/")) s.substring(0,s.length -1) else s)

      // make path relative
      val fsFiles = org.apache.commons.io.FileUtils.listFilesAndDirs(
        testDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE
      ).toList.map(_.getPath).map(s ⇒ "." + s.substring(s.indexOf("/testdir")))

      files should equal (fsFiles)

    }

    "retain file integrity of archives" in {
      // only test file sizes
      val testDir = new File(getClass.getResource("/testdir").getPath)
      val outFile = System.getProperty("java.io.tmpdir") + "/test.tar.gz"
      val tarFile = SbtScpBackup.createArchive(testDir, outFile).left.map(e ⇒ fail(e)).right.get

      val files = Unpack(tarFile.getAbsolutePath).filter(!_.isDirectory)
        .map(a ⇒ a.getName → a.getSize)

      // make path relative
      val fsFiles = org.apache.commons.io.FileUtils.listFilesAndDirs(
        testDir, TrueFileFilter.TRUE, TrueFileFilter.TRUE
      ).toList.filter(!_.isDirectory).map(f ⇒ f.getPath → f.length).map(s ⇒ ("." + s._1.substring
        (s._1.indexOf("/testdir"))) → s._2)

      files should equal (fsFiles)
    }
  }
}


/**
 * Adapted from https://gist.github.com/ayosec/6853615
 */
object Unpack {

  def apply(path:String): List[ArchiveEntry] = {
    def uncompress(input: BufferedInputStream): InputStream =
      Try(new CompressorStreamFactory().createCompressorInputStream(input)) match {
        case Success(i) => new BufferedInputStream(i)
        case Failure(_) => input
      }

    def extract(input: InputStream): ArchiveInputStream =
      new ArchiveStreamFactory().createArchiveInputStream(input)

    val input = extract(uncompress(new BufferedInputStream(new FileInputStream(path))))
    def stream: Stream[ArchiveEntry] = input.getNextEntry match {
      case null => Stream.empty
      case entry => entry #:: stream
    }
    stream.toList
  }
}