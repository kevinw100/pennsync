package com.pennsync.client

import java.nio.file.{Path, Paths}

import better.files.File
import com.pennsync.MetaFile
import io.methvin.better.files._
import java.io

import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Singleton Watcher object (be sure to make sure that access to this is thread-safe)
  */
object WatchDirScala{

  private var instance : WatchDirScala = null
  def create(watchDir: Path)(implicit serverConnection: ServerConnection) : WatchDirScala = {
    if(instance == null) {
      instance = new WatchDirScala(watchDir)
    }
    instance
  }

  def pauseWatcher() : Unit = {
    instance.pause()
  }

  def startWacher() : Unit = {
    instance.start
  }

}

class WatchDirScala(baseDir: Path)(implicit serverConnection: ServerConnection){
  val watchDir = File(baseDir)

  private def handleCreate(file: better.files.File): Unit = {
    val fileMetaData = MetaFile.getMetaData(baseDir, file.toJava)
    Client.addToLedger(fileMetaData)
    serverConnection.sendFile(fileMetaData, file.toJava, RequestDataFactory.AddFileRequest)
    //TODO: STUB FOR SENDING FILE TO SERVER
  }

  private def handleModify(file: better.files.File) : Unit = {
    val fileMetaData = MetaFile.getMetaData(baseDir, file.toJava)
    Client.modifyLedgerEntry(fileMetaData)
    if (!ClientLedger.isIgnoredFile(fileMetaData.relativePath)) {
      serverConnection.sendFile(fileMetaData, file.toJava, RequestDataFactory.ModifyFileRequest)
    }
  }

  private def handleDelete(file: better.files.File) : Unit = {
    val fileMetaData = MetaFile.getMetaData(baseDir, file.toJava)
    Client.removeFromLedger(fileMetaData)
    serverConnection.sendUntrackRequest(fileMetaData)
  }

  var watcher : RecursiveFileMonitor = null

  def start() : Unit = {
    if (watcher == null) {
      watcher = createWatcher()
      watcher.start
    }
  }

  def pause() : Unit = {
    if(watcher != null){
      watcher.close()
      watcher = null
    }
  }

  private def createWatcher(): RecursiveFileMonitor = {
    new RecursiveFileMonitor(watchDir) {
      override def onCreate(file: better.files.File, count: Int) = handleCreate(file)
      override def onModify(file: better.files.File, count: Int) = handleModify(file)
      override def onDelete(file: better.files.File, count: Int) = handleDelete(file)
    }
  }
}
