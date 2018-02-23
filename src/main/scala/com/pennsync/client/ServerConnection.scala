package com.pennsync.client

import java.nio.file.Path

import com.pennsync.MetaFile
import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}

object ServerConnection{
  def createConnection(options : SSHOptions) : ServerConnection = {
    new ServerConnection(options)
  }
}

class ServerConnection(options: SSHOptions){
  implicit val ssh: SSH = new SSH(options)
  val sftp: SSHFtp = new SSHFtp()

  def mapToServerPath(relPath: String) : String = {
    //TODO: Use nio paths to properly augment
    "pending/" ++ relPath
  }

  def sendFile(metaData: MetaFile) : Unit = {
    if(sftp == null) {
      //Do nothing (for testing)
      println("[WARN] SFTP connection is null. @Devs please make sure to hook up the Pi")
      return
    }
    val serverPath = mapToServerPath(metaData.relativePath)
    // Modify to send javafile .toFile
    sftp.send(serverPath)
    //TODO: Send HTTPRequest to tell Server that file has completed sending and Server will then check Client JSON
  }

}