package com.pennsync

import java.nio.file.Path

object ClientLedger{
  def create(ledgerParser: LedgerParser, syncDir: Path): ClientLedger = {
    implicit val parser = ledgerParser
    val fromLedger = ledgerParser.fromLedgerFile()
    val fromFS = ledgerParser.fromFS(syncDir)

    //fromLedger AFTER fromFS b/c fromLedger metadata is MORE accurate than fromFS (want to take info from ledger Over info from FS)
    ClientLedger(fromFS ++ fromLedger)
  }
}
case class ClientLedger(pathsToMetadata: Map[String, MetaFile])(implicit ledgerParser: LedgerParser) {
  lazy val fileMetaData = pathsToMetadata.values.toList
  def addFile(metadata: MetaFile) : ClientLedger = {
    if(pathsToMetadata.contains(metadata.relativePath)){
      this
    }
    else{
      ClientLedger(pathsToMetadata + (metadata.relativePath -> metadata))
    }
  }

  def updateFileMetadata(metaData: MetaFile) : ClientLedger = {
    if(!pathsToMetadata.contains(metaData.relativePath)){
      println("Error: Request to update a non-existing metadata file, doing no updates")
      this
    }
    else{
      ClientLedger(pathsToMetadata + (metaData.relativePath -> metaData))
    }
  }

  def deleteFileMetadata(metaData: MetaFile) : ClientLedger = {
    ClientLedger(pathsToMetadata - (metaData.relativePath))
  }

  /**
    * Used to write data back to the ledger file (persist ledger mapping)
    */
  def write() = {
    ledgerParser.writeToFile(pathsToMetadata.values.toList)
  }
}
