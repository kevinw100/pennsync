package com.pennsync

case class ServerLedger(clientWithMachine: Map[Machine, ClientLedger]){
  // Maps a File to the machines that are currently tracking it
  lazy val filesToClients : Map[String, Set[Machine]] = {
    //Gets all the relative paths stored on the server side
    def allRelPaths : Set[String] = clientWithMachine.values.flatMap(_.pathsToMetadata).map(_._1).toSet

    allRelPaths.map(path =>
      (path,
        clientWithMachine.filter{case (_, ledger) =>
        ledger.pathsToMetadata.contains(path)
      }.keySet)
    ).toMap

  }

  lazy val clientIpMap : Map[String, ClientLedger] = clientWithMachine.map{case (k, v) => (k.ip, v)}
  lazy val clientIdMap : Map[Long, ClientLedger] = clientWithMachine.map{case (k,v) => (k.id, v)}
  lazy val clientIpLookup: Map[String, Machine] = clientWithMachine.map{case (k, _) => (k.ip, k)}
  lazy val clientIdLookup: Map[Long, Machine] = clientWithMachine.map{case (k, _) => (k.id, k)}

  def getLedgerByClientIp(ip : String) : Option[ClientLedger] = clientIpMap.get(ip)
  def getLedgerByClientId(id: Int) : Option[ClientLedger] = clientIdMap.get(id)

  def addClient(clientData: Machine, clientLedger: ClientLedger) : ServerLedger = {
    ServerLedger(clientWithMachine + (clientData -> clientLedger))
  }

  def removeClient(clientData: Machine) : ServerLedger = ServerLedger(clientWithMachine - clientData)

  /**
    * Updates a client mapping to delete a given item
    * @param client
    * @param toDeletePath relative
    * @return
    */
  def updateClientByDeletingFile(client: Machine, toDeletePath: String) : ServerLedger = {
    val tgtLedger : Option[ClientLedger] = clientWithMachine.get(client)
    val tgtLedgerFiles = tgtLedger.map(_.fileMetaData)
    val removedLedger = tgtLedgerFiles.map(metafiles => metafiles.filterNot(_ == toDeletePath))
    removedLedger match{
      case Some(updatedLedger) => //TODO: Impl
                                  ???
      case None => print(s"[WARN] : filePath ${toDeletePath} was not found on machine $client"); this
    }
  }

  def updateClientByAddingFile(client: Machine, fileMetaData: MetaFile) : ServerLedger = {
    val tgtLedger : Option[ClientLedger] = clientWithMachine.get(client)
    val tgtLedgerFiles = tgtLedger.map(_.fileMetaData)
    val removedLedger = tgtLedgerFiles.map(metafiles => metafiles :+ fileMetaData)
    removedLedger match{
      case Some(updatedLedger) => ServerLedger(clientWithMachine + (client -> ClientLedger(updatedLedger)))
      case None => print(s"[WARN] : filePath ${fileMetaData.relativePath} was not found on machine $client"); this
    }
  }
}
