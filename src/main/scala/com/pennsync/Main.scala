package com.pennsync

import java.io.File

import fr.janalyse.ssh.{SSH, SSHOptions}

object Main extends App {
  if (args.length != 2) {
    val sshOpt: SSHOptions = SSHOptions("10.215.150.241", "pi", "pi")

    implicit val connec: SSH = new SSH(sshOpt)

    println(connec.pwd)
    println("I need a file name and an ip address doofus!")
  } else {
    val sshOpt: SSHOptions = SSHOptions("10.215.150.241", "pi", "pi")

    implicit val connec: SSH = new SSH(sshOpt)

    println(connec.pwd)

    println(args(0))
    println(args(1))

    val appDirPath = System.getProperty("user.dir")

    val appDir = new File(appDirPath)

    DirList.getFiles(appDir, appDirPath)

  }


}
