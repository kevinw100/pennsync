package com.pennsync.server

import com.pennsync.MetaFile

/**
  * Used by the com.pennsync.server to track file metadata
 *
  * @param pathToDataMap = a mapping from relativePath -> (metadata, clients that track the given file)
  */
case class ServerLedger(pathToDataMap: Map[String, (MetaFile, Set[Machine])]){

  /**
    * Used to determine if a machine is already tracking a given file (for conflict checking), below methods should be
    * called AFTER making sure no conflicts have occurred
    * @param client
    * @param filePath
    */
  def isClientTracking(client: Machine, filePath: String): Boolean = {
    val clientsOpt: Option[Set[Machine]] = pathToDataMap.get(filePath).map(_._2)
    val clientsContainsClientOpt : Option[Boolean] = clientsOpt.map(_.contains(client))
    clientsContainsClientOpt.getOrElse(false)
  }

  // Called when an AddEvent occurs client-side (their version considered to be the most recent), can also handle modify
  def handleClientAdd(client: Machine, metaData: MetaFile) : ServerLedger = {
    val relPath = metaData.relativePath

    // orElse occurs when you receive a new file that has never been tracked by a previous machine
    val clients : Set[Machine] = pathToDataMap.getOrElse(relPath, (metaData, Set[Machine]()))._2
    val mappedValue = (metaData, clients + client)
    ServerLedger(pathToDataMap + (relPath -> mappedValue))
  }

  def handleClientUntrack(client: Machine, relPath: String) : ServerLedger = {
    pathToDataMap.get(relPath) match{
      case Some((metaData, clients)) =>
        if((clients - client).size > 0) {
          ServerLedger(pathToDataMap + (relPath -> (metaData, clients - client)))
        }
        else{
          //Reached when a file has no clients that are tracking it
          //TODO: write something that deletes the file on the Server's actual FS

          //update the serverledger to no longer track the file
          ServerLedger(pathToDataMap - relPath)
        }
      case None => println(s"[ERROR]: Received request to delete file $relPath, which does not exist on the com.pennsync.server side, doing nothing")
        this
    }
  }

  //Occurs when the client
  def handleClientModify(client: Machine, metaData: MetaFile) : ServerLedger = {
    //Should work
    handleClientAdd(client, metaData)
  }

  //Called when the client wants to track a file, we should NOT update the MetaFile entry in the com.pennsync.server's ledger file
  def handleClientTrack(client: Machine, relPath: String) : ServerLedger = {
    pathToDataMap.get(relPath) match {
      case Some((metadata, clients)) => ServerLedger(pathToDataMap + (relPath -> (metadata, clients + client)))
      case None => println(s"[ERROR]: Received request to track file $relPath which does not exist com.pennsync.server side, making no changes")
        this
    }
  }

  // A naive implementation that calls handleClientUntrack to every single file that the com.pennsync.server tracks! (Add client is trickier)
  def removeClient(client: Machine) : ServerLedger = {
    val relPaths = pathToDataMap.keys
    relPaths.foldLeft(this)((acc, currPath) => acc.handleClientUntrack(client, currPath))
  }
}
