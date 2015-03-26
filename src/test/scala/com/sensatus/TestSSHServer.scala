package com.sensatus

import java.io.File
import java.net.ServerSocket
import java.security.PublicKey

import org.apache.sshd.SshServer
import org.apache.sshd.common.NamedFactory
import org.apache.sshd.common.keyprovider.FileKeyPairProvider
import org.apache.sshd.server.command.ScpCommandFactory
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.{PublickeyAuthenticator, UserAuth}
import org.apache.sshd.server.auth.UserAuthPublicKey

import scala.util.Try

/**
 * Created by max on 26/03/15.
 */
object TestSSHServer {

  // setup basic server and find avaliable port
  private val sshd = SshServer.setUpDefaultServer()
  val port = findFreePort().getOrElse(throw new Exception("Cannot find free port"))
  sshd.setPort(port)

  // use a hostkey for identification
  private val hostKey = new File(getClass.getResource("/keys/hostkey.pem").toURI)
  sshd.setKeyPairProvider(new FileKeyPairProvider(List(hostKey.toString).toArray))

  // create dummy authentication method
  sshd.setPublickeyAuthenticator(new PublickeyAuthenticator {
    override def authenticate(username: String, key: PublicKey, session: ServerSession): Boolean
    = true
  })

  // allow scp
  sshd.setCommandFactory(new ScpCommandFactory())

  def start() = {
    sshd.start()
  }
  def stop() = {
    sshd.stop()
  }

 private def findFreePort():Try[Int] = {
    Try {
      val socket = new ServerSocket(0)
      try {
        socket.getLocalPort
      } finally {
        socket.close()
      }
    }
  }
}
