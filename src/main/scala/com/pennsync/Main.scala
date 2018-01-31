package com.pennsync

import java.io.File
import java.io.PrintWriter
import java.nio.file.{Files, Path, Paths}

import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}
import net.liftweb.json._

/**
  * This is the Client-side main
  */
object Main extends App {
  // TODO: change these cli arguments (need a way to store ledger outside of synced directory?)
  def usage() = {
    var usageString = ""
    for(argstring <- args) usageString +=  argstring + " "
    println(s"ERROR: incorrect number of arguments: $usageString")
    println("Usage: run [root directory] [path to ledger file (or path to directory where ledger file is to be created)] [server ip]")
  }

  if(args.length != 3){
    usage()
    System.exit(0)
  }

  //Finds the absolute path of the synced folder
  val syncedDirAbs = Paths.get(args(0)).toRealPath()
  println(s"synced Directory: ${syncedDirAbs.toString}")

  val ledgerDirAbs = Paths.get(args(1)).toRealPath()
  println(s"ledger directory: ${ledgerDirAbs.toString}")


  val ledgerPath : String = {
    /**
      * If statement is here because it's possible that
      */
    if(Files.isDirectory(ledgerDirAbs)){
      //Creates [ledger dir]/ledger.json path
      ledgerDirAbs.resolve("ledger.json").toString
    }
    else{
      ledgerDirAbs.toString
    }
  }

  implicit val formats : Formats = DefaultFormats

  val clientLedger = ClientLedger.create(ledgerPath, syncedDirAbs)
  //Used as a dummy value
  val clientMachine = Machine(0, "some_default_ip")

  def createWatchDir(rootDir: Path) = {
    new WatchDir(rootDir)
  }

  val serverLedger = clientLedger.pathsToMetadata.values.foldLeft(ServerLedger(Map[String, (MetaFile, Set[Machine])]()))((acc, x) => acc.handleClientAdd(clientMachine, x))

  println(s"The information contained in serverLedger: ${serverLedger.pathToDataMap}")

  LedgerParser.writeToServerFile(serverLedger.pathToDataMap, System.getProperty("user.dir"))
  println("Finished writing!")

//  TODO: uncomment when connected to the pi
//  // 10.215.150.241
//  val sshOpt: SSHOptions = SSHOptions(args(2), "pi", "pi")
//
//  implicit val sshConnect: SSH = new SSH(sshOpt)
//
//  println(sshConnect.pwd)
//
//  val sftpConnect: SSHFtp = new SSHFtp()
//
//  sftpConnect.send(args(0))
//
//
//
//  sftpConnect.close()
//  sshConnect.close()

//  TODO: Uncomment below
//  val watcher = createWatchDir(syncedDirAbs)
//  watcher.processEvents(syncedDirAbs, clientLedger)

}
