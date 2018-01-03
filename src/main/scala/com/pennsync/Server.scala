package com.pennsync

object Server{
  def create(): Server = {
    Server(ServerLedgerFile(Map()))
  }
}

case class Server(ledgerFile: ServerLedgerFile){
  lazy val metaFileSet : Set[MetaFile] = ledgerFile.clientWithMachine.values.flatMap(file => file.fileMetadata).toSet
  def addClient(client: Machine, clientLedger: LedgerFile) : Server = {
    Server(ledgerFile.addClient(client, clientLedger))
  }

  def removeClient(client: Machine) : Server = {
    Server(ledgerFile.removeClient(client))
  }

  def updateClientByAddingFile(client: Machine, fileMetaData: MetaFile) : Server = {
    Server(ledgerFile.updateClientByAddingFile(client, fileMetaData))
  }

  def updateClientByRemovingFile(client: Machine, fileMetaData: MetaFile) : Server = {
    Server(ledgerFile.updateClientByDeletingFile(client, fileMetaData))
  }

  /**
    * Looks through both the server and the client Metafiles and determines the nature of the update
    * @param serverFiles MetaFiles that the server is keeping track of
    * @param clientFiles Metafiles that the client is keeping track of
    */
  private def determineFileStatuses(serverFiles: List[MetaFile], clientFiles: List[MetaFile]) : List[(MetaFile, FileCleanlinessStatus)] = {
    val serverMap = serverFiles.map(metafile => (metafile.relativePath, metafile)).toMap
    val clientMap = clientFiles.map(metafile => (metafile.relativePath, metafile)).toMap
    (serverFiles ++ clientFiles).map(_.relativePath).toSet.map { path : String =>
      val serverFile : Option[MetaFile] = serverMap.get(path)
      val clientFile : Option[MetaFile] = clientMap.get(path)
      val metafileData = (serverFile, clientFile)
      (serverFile, clientFile) match {
        case (Some(presentServerFile: MetaFile), Some(presentClientFile: MetaFile)) =>
          val serverLastUpdate = presentServerFile.lastUpdateLong
          val clientLastUpdate = presentClientFile.lastUpdateLong
          if (serverLastUpdate > clientLastUpdate) {
            (presentServerFile, ClientOutOfDate())
          }
          else if (serverLastUpdate == clientLastUpdate) {
            (presentClientFile, CleanFile())
          }
          else {
            (presentClientFile, ClientUpdatedFile())
          }

        case (Some(presentServerFile: MetaFile), None) => (presentServerFile, ClientDeletedFile())
        case (None, Some(presentClientFile: MetaFile)) =>
          //3 possibilities: client could have created a new file or renamed a file
          //Strategy for resolution: Check to see if the newly created file will interfere with the server's ledger.
          //If there is a conflict, then put in unsureOfFileChange
          if (metaFileSet.contains(presentClientFile))
            (presentClientFile, ClientCausedConflict())
          else
            (presentClientFile, ClientRenamedOrCreatedFile())
        case (_, _) => print(s"[ERROR] in Server.determineFileStatuses(): File $path was NOT found in either server nor client ledger file")
          (null, null)
      }
    }.toList
  }

  /**
    * Pulls changes from the client (Will not issue any requests to sync with the client, but WILL warn in the case of a conflict)
    * @param client the Client's Machine info
    * @param clientLedger The CLIENT'S ledgerfile (not the one stored in the server)
    * @return a new server state
    */
  def getClientChanges(client: Machine, clientLedger: LedgerFile) : Server = {
    val updatedServerFile = (for{
      serverFile <- ledgerFile.clientWithMachine.get(client)
      filesWithStatuses = determineFileStatuses(serverFile.fileMetadata, clientLedger.fileMetadata)
      filesToPull = filesWithStatuses.flatMap{ case (file, status) =>
          status match{
            case _ : ClientDeletedFile | _ : ClientRenamedOrCreatedFile | _ : ClientUpdatedFile => Some(file)
            case _ : ClientCausedConflict => print(s"[CONFLICT]: Client's file ${file.relativePath} already exists on the server")
              //TODO: issue a warning to the client!
              None
            case _ => None
          }
      }
    } yield filesToPull).getOrElse(clientLedger.fileMetadata)

    val updatedLedgerEntry = ledgerFile.clientWithMachine + (client -> LedgerFile(updatedServerFile))
    Server(ServerLedgerFile(updatedLedgerEntry))
  }
}

trait FileCleanlinessStatus
case class CleanFile() extends FileCleanlinessStatus
case class ClientDeletedFile() extends FileCleanlinessStatus
case class ClientRenamedOrCreatedFile() extends FileCleanlinessStatus
case class ClientCausedConflict() extends FileCleanlinessStatus
case class ClientOutOfDate() extends FileCleanlinessStatus
case class ClientUpdatedFile() extends FileCleanlinessStatus