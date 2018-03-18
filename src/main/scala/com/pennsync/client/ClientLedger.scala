package com.pennsync.client

import java.nio.file.Path

import com.pennsync.MetaFile
import net.liftweb.json.Formats

object ClientLedger{

  def isIgnoredFile(relPath: String) : Boolean = {
    //TODO: Make more robust
    relPath == "ledger.json"
  }
  //Formats used to do ledgerParser tasks
  def create(ledgerPath: String, syncDir: Path)(implicit format: Formats): ClientLedger = {
    val fromLedger: Map[String, MetaFile] = LedgerParser.fromClientLedgerFile(ledgerPath).filterNot{case (path, _) => isIgnoredFile(path)}
    val fromFS: Map[String, MetaFile] = LedgerParser.getFilesFromSyncDir(syncDir).filterNot{case (path, _) => isIgnoredFile(path)}

    //fromLedger AFTER fromFS b/c fromLedger metadata is MORE accurate than fromFS (want to take info from ledger Over info from FS)
    val ledger = ClientLedger(fromFS ++ fromLedger, ledgerPath)
    ledger.write()
    ledger
  }
}


case class ClientLedger(pathsToMetadata: Map[String, MetaFile], ledgerPath: String)(implicit format: Formats){

  lazy val fileMetaData = pathsToMetadata.values.toList
  def addFile(metadata: MetaFile) : ClientLedger = {
    if(pathsToMetadata.contains(metadata.relativePath) || ClientLedger.isIgnoredFile(metadata.relativePath)){
      this
    }
    else{
      ClientLedger(pathsToMetadata + (metadata.relativePath -> metadata), ledgerPath)
    }
  }

  def updateFileMetadata(metaData: MetaFile) : ClientLedger = {
    if(ClientLedger.isIgnoredFile(metaData.relativePath)){
      println("Found a file that is being ignored!")
      this
    }
    else{
      ClientLedger(pathsToMetadata + (metaData.relativePath -> metaData), ledgerPath)
    }
  }

  def deleteFileMetadata(metaData: MetaFile) : ClientLedger = {
    ClientLedger(pathsToMetadata - (metaData.relativePath), ledgerPath)
  }

  /**
    * Used to write data back to the ledger file (persist ledger mapping)
    */
  def write() = {
    LedgerParser.writeToClientFile(pathsToMetadata.values.toList, ledgerPath)
  }
}
