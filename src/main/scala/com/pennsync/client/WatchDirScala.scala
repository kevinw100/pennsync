package com.pennsync.client

import java.nio.file.{Path, Paths}

import better.files.File
import com.pennsync.MetaFile
import io.methvin.better.files._
import java.io

import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}

import scala.concurrent.ExecutionContext.Implicits.global


object WatchDirScala{
  def create(watchDir: Path)(implicit serverConnection: ServerConnection) : WatchDirScala = {
    new WatchDirScala(watchDir)
  }
}

class WatchDirScala(baseDir: Path)(implicit serverConnection: ServerConnection){
  val watchDir = File(baseDir)

  def getMetaData(file: better.files.File) : MetaFile = {
    val child = file.path
    val childFile : java.io.File = new java.io.File(child.toString)
    val relPath = baseDir.relativize(child)
    val lastModifiedLong = childFile.lastModified()
    val lastModified = DirList.getUTCTimeString(lastModifiedLong)
    MetaFile(relPath.toString, lastModified, lastModifiedLong)
  }

  def handleCreate(file: better.files.File): Unit = {
    val fileMetaData = getMetaData(file)
    Client.addToLedger(fileMetaData)
    serverConnection.sendFile(fileMetaData, file.toJava)
    //TODO: STUB FOR SENDING FILE TO SERVER
  }

  def handleModify(file: better.files.File) : Unit = {
    val fileMetaData = getMetaData(file)
    Client.modifyLedgerEntry(fileMetaData)
    serverConnection.sendFile(fileMetaData, file.toJava)
    // TODO: Send file over to server and do ledger checking
  }

  def handleDelete(file: better.files.File) : Unit = {
    val fileMetaData = getMetaData(file)
    Client.removeFromLedger(fileMetaData)
    //TODO: Send untrack command to the server
  }

  val watcher = new RecursiveFileMonitor(watchDir) {
    override def onCreate(file: better.files.File, count: Int) = handleCreate(file)
    override def onModify(file: better.files.File, count: Int) = handleModify(file)
    override def onDelete(file: better.files.File, count: Int) = println(s"$file got deleted")
  }

  def start() : Unit = {
    watcher.start()
  }
}
