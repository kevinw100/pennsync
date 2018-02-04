package com.pennsync

import java.io.File
import java.io.PrintWriter

import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}
import net.liftweb.json._


object Main extends App {
  if (args.length != 2) {
    //println("I need a file name and an ip address doofus!")

    implicit val formats = DefaultFormats
    val ledgerString = scala.io.Source.fromFile("ledger.json").mkString
    //println(ledgerString)

    val ledgerJson = parse(ledgerString)
    val ledgerList: List[MetaFile] = ledgerJson.extract[List[MetaFile]]

    val ledgerMap: Map[String, MetaFile] = ledgerList.map{case x : MetaFile => (x.relativePath, x)}.toMap


    val appDirPath = System.getProperty("user.dir")

    val appDir = new File(appDirPath)

    // List all files in directory
    val newMap = DirList.getFiles(appDir, appDirPath, ledgerMap)

    //println(newMap)
    val ledgerNewList: List[MetaFile] = newMap.map(_._2).toList
    //println(ledgerNewList)

    val ledgerNewString: String = Serialization.write(ledgerNewList)

    new PrintWriter("ledger.json") { write(ledgerNewString); close }

  } else {
    val sshOpt: SSHOptions = SSHOptions(args(1), "pi", "pi")

    implicit val sshConnect: SSH = new SSH(sshOpt)

    //println(sshConnect.pwd)

    val sftpConnect: SSHFtp = new SSHFtp()

    sftpConnect.send(args(0))



    sftpConnect.close()
    sshConnect.close()

  }


}
