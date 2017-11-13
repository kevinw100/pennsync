package com.pennsync

import java.io.File

import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}

object Main extends App {
  if (args.length != 2) {
    println("I need a file name and an ip address doofus!")
  } else {
    // 10.215.150.241
    val sshOpt: SSHOptions = SSHOptions(args(1), "pi", "pi")

    implicit val sshConnect: SSH = new SSH(sshOpt)

    println(sshConnect.pwd)

    val sftpConnect: SSHFtp = new SSHFtp()

    sftpConnect.send(args(0))

    val appDirPath = System.getProperty("user.dir")

    val appDir = new File(appDirPath)

    // List all files in directory
    //DirList.getFiles(appDir, appDirPath)

    sftpConnect.close()
    sshConnect.close()

  }


}
