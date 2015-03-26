package com.sensatus

import java.io.File

import com.decodified.scalassh.{HostConfig, PublicKeyLogin}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

/**
 * Created by max on 26/03/15.
 */
class SSHTests extends WordSpec with Matchers with BeforeAndAfterAll {


  override def beforeAll(): Unit = {
    TestSSHServer.start()
  }
  override def afterAll(): Unit = {
    TestSSHServer.stop()
  }


  "sbt-scp-backup scp client" must {
    "accept files over scp" in {
      val keyfile = new File(getClass.getResource("/keys/hostkey.pem").toURI)
      val testFile = new File(getClass.getResource("/testdir.tar.gz").toURI)
      val h = HostConfig(PublicKeyLogin("test", keyfile.toString), "localhost", TestSSHServer.port)
      val result = SbtScpBackup.scpArchive(h, testFile, "/tmp/")
      result.left.map(println)
      result.isRight should be(true)
      val f = new File("/tmp/testdir.tar.gz")
      f.exists() should be(true)
      f.delete()
    }
  }
}
