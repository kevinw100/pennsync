package com.pennsync.client

import java.nio.file.{Files, Path, Paths}

import net.liftweb.json._
import com.pennsync.server.Machine
import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}

/**
  * This is the Client-side main
  */
object Main extends App {
  // TODO: change these cli arguments (need a way to store ledger outside of synced directory?)
  def usage() : Unit = {
    var usageString = ""
    for(argstring <- args) usageString +=  argstring + " "
    println(s"ERROR: incorrect number of arguments: $usageString")
    println("Usage: run [root directory] [path to ledger file (or path to directory where ledger file is to be created)] [com.pennsync.server ip]")
  }

  if(args.length != 3){
    usage()
    System.exit(0)
  }

  //TODO: Refactor so that this code all ends up in the Client class

  //Finds the absolute path of the synced folder
  val syncedDirAbs: Path = Paths.get(args(0)).toRealPath()
  println(s"synced Directory: ${syncedDirAbs.toString}")

  val ledgerDirAbs: Path = Paths.get(args(1)).toRealPath()
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
  val clientMachine: Machine = Machine(0, "some_default_ip")

  Client.setClientLedger(clientLedger)

  def createWatchDir(rootDir: Path) = {
    new WatchDir(rootDir)
  }


//  TODO: Uncomment below
//  val watcher = createWatchDir(syncedDirAbs)
//  watcher.processEvents(syncedDirAbs, clientLedger)
//  watcher.processEvents()
// 10.215.149.8
  val sshOpt: SSHOptions = SSHOptions("10.215.151.112", "pi", "pi")
  implicit val conn : ServerConnection = ServerConnection.createConnection(sshOpt)

// TODO: Testing WatchDirScala
  val watcher = new WatchDirScala(syncedDirAbs)
  watcher.start()

  while(1 > 0){}

//  sftpConnect.close()
//  sshConnect.close()
}
