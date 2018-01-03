package com.pennsync

import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}
import net.liftweb.json._
import java.io.File

object LedgerFile{
  def createFromOld(ledgerString : String)(implicit format: Formats): LedgerFile ={
    val ledgerJson = parse(ledgerString)
    val ledgerList: List[MetaFile] = ledgerJson.extract[List[MetaFile]]

    val ledgerMap: Map[String, MetaFile] = ledgerList.map{case x : MetaFile => (x.relativePath, x)}.toMap

    val appDirPath = System.getProperty("user.dir")
    val appDir = new File(appDirPath)

    // List all files in directory
    val newMap = DirList.getFiles(appDir, appDirPath, ledgerMap)

    val ledgerNewList: List[MetaFile] = newMap.values.toList

    LedgerFile(ledgerNewList)
  }

}

/**
  * The Client LedgerFile
  * @param fileMetadata a list of file Metadata that the Client Ledger tracks
  */
case class LedgerFile(fileMetadata: List[MetaFile])

/**
  * Server's Metadata Entry.
  * @param clientWithMachine: Contains the Machine (way of id'ing client) with Corresponding LedgerFile.
  */
case class ServerLedgerFile(clientWithMachine: Map[Machine, LedgerFile]){

  lazy val clientIpMap : Map[String, LedgerFile] = clientWithMachine.map{case (k, v) => (k.ip, v)}
  lazy val clientIdMap : Map[Long, LedgerFile] = clientWithMachine.map{case (k,v) => (k.id, v)}
  lazy val clientIpLookup: Map[String, Machine] = clientWithMachine.map{case (k, _) => (k.ip, k)}
  lazy val clientIdLookup: Map[Long, Machine] = clientWithMachine.map{case (k, _) => (k.id, k)}

  def getLedgerByClientIp(ip : String) : Option[LedgerFile] = clientIpMap.get(ip)
  def getLedgerByClientId(id: Int) : Option[LedgerFile] = clientIdMap.get(id)

  def addClient(clientData: Machine, clientLedger: LedgerFile) : ServerLedgerFile = {
    ServerLedgerFile(clientWithMachine + (clientData -> clientLedger))
  }

  def removeClient(clientData: Machine) : ServerLedgerFile = ServerLedgerFile(clientWithMachine - clientData)

  def updateClientByDeletingFile(client: Machine, fileMetaData: MetaFile) : ServerLedgerFile = {
    val tgtLedger : Option[LedgerFile] = clientWithMachine.get(client)
    val tgtLedgerFiles = tgtLedger.map(_.fileMetadata)
    val removedLedger = tgtLedgerFiles.map(metafiles => metafiles.filterNot(_ == fileMetaData))
    removedLedger match{
      case Some(updatedLedger) => ServerLedgerFile(clientWithMachine + (client -> LedgerFile(updatedLedger)))
      case None => print(s"[WARN] : filePath ${fileMetaData.relativePath} was not found on machine $client"); this
    }
  }

  def updateClientByAddingFile(client: Machine, fileMetaData: MetaFile) : ServerLedgerFile = {
    val tgtLedger : Option[LedgerFile] = clientWithMachine.get(client)
    val tgtLedgerFiles = tgtLedger.map(_.fileMetadata)
    val removedLedger = tgtLedgerFiles.map(metafiles => metafiles :+ fileMetaData)
    removedLedger match{
      case Some(updatedLedger) => ServerLedgerFile(clientWithMachine + (client -> LedgerFile(updatedLedger)))
      case None => print(s"[WARN] : filePath ${fileMetaData.relativePath} was not found on machine $client"); this
    }
  }
}

