package com.pennsync.client

import java.nio.file.Path

import com.pennsync.MetaFile
import net.liftweb.json.Formats

object ClientLedger{

  //Formats used to do ledgerParser tasks
  def create(ledgerPath: String, syncDir: Path)(implicit format: Formats): ClientLedger = {
    val fromLedger = LedgerParser.fromClientLedgerFile(ledgerPath)
    val fromFS = LedgerParser.getFilesFromSyncDir(syncDir)

    //fromLedger AFTER fromFS b/c fromLedger metadata is MORE accurate than fromFS (want to take info from ledger Over info from FS)
    ClientLedger(fromFS ++ fromLedger, ledgerPath)
  }
}

case class ClientLedger(pathsToMetadata: Map[String, MetaFile], ledgerPath: String)(implicit format: Formats){

  lazy val fileMetaData = pathsToMetadata.values.toList
  def addFile(metadata: MetaFile) : ClientLedger = {
    if(pathsToMetadata.contains(metadata.relativePath)){
      this
    }
    else{
      ClientLedger(pathsToMetadata + (metadata.relativePath -> metadata), ledgerPath)
    }
  }

  def updateFileMetadata(metaData: MetaFile) : ClientLedger = {
    if(!pathsToMetadata.contains(metaData.relativePath)){
      println("Error: Request to update a non-existing metadata file, doing no updates")
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
