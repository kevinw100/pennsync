package com.pennsync.client

import java.nio.file.Path

import better.files.File
import io.methvin.better.files._
import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}

class WatchDirScala(dirPath: Path) {
  val watchDir = File(dirPath)

  def sendSSH(fileName: String): Unit = {
    // 10.215.149.8
    val sshOpt: SSHOptions = SSHOptions("10.215.149.8", "pi", "pi")

    implicit val sshConnect: SSH = new SSH(sshOpt)

    println(sshConnect.pwd)

    val sftpConnect: SSHFtp = new SSHFtp()

    sftpConnect.send(fileName)

    sftpConnect.close()
    sshConnect.close()
  }

  val watcher = new RecursiveFileMonitor(watchDir) {
    override def onCreate(file: File, count: Int) = sendSSH(file.name)
    override def onModify(file: File, count: Int) = println(s"$file got modified $count times")
    override def onDelete(file: File, count: Int) = println(s"$file got deleted")
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  watcher.start()

}
