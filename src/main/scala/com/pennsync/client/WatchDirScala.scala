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

  def handleCreate(file: better.files.File): Unit = {
    val fileMetaData = MetaFile.getMetaData(baseDir, file.toJava)
    Client.addToLedger(fileMetaData)
    serverConnection.sendFile(fileMetaData, file.toJava, RequestDataFactory.AddFileRequest)
    //TODO: STUB FOR SENDING FILE TO SERVER
  }

  def handleModify(file: better.files.File) : Unit = {
    val fileMetaData = MetaFile.getMetaData(baseDir, file.toJava)
    Client.modifyLedgerEntry(fileMetaData)
    if (!ClientLedger.isIgnoredFile(fileMetaData.relativePath)) {
      serverConnection.sendFile(fileMetaData, file.toJava, RequestDataFactory.ModifyRequest)
    }
    // TODO: Send file over to server and do ledger checking
  }

  def handleDelete(file: better.files.File) : Unit = {
    val fileMetaData = MetaFile.getMetaData(baseDir, file.toJava)
    Client.removeFromLedger(fileMetaData)
    //TODO: Send untrack command to the server
  }

  val watcher = new RecursiveFileMonitor(watchDir) {
    override def onCreate(file: better.files.File, count: Int) = handleCreate(file)
    override def onModify(file: better.files.File, count: Int) = handleModify(file)
    override def onDelete(file: better.files.File, count: Int) = handleDelete(file)
  }

  def start() : Unit = {
    watcher.start()
  }
}
