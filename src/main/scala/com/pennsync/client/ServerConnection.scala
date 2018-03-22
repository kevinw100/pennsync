package com.pennsync.client

import java.nio.file.{Path, Paths}

import com.pennsync.MetaFile
import com.twitter.finagle.http
import com.twitter.util.{Await, Future}
import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}
import net.liftweb.json.Formats
import com.jcraft.jsch.SftpException

object ServerConnection{
  def createConnection(options : SSHOptions, syncDir: Path)(implicit  formats: Formats) : ServerConnection = {
    new ServerConnection(options, syncDir)
  }
}

/**
  * Class used to interface with the server
  * @param options
  * @param formats
  */
class ServerConnection(options: SSHOptions, syncDir: Path)(implicit formats: Formats){
  implicit val ssh: SSH = new SSH(options)
  val sftp: SSHFtp = new SSHFtp()

  //Used as a dev shortcut
  private def isConnected() : Boolean = {
    if(sftp == null) {
      //Do nothing (for testing)
      println("[WARN] SFTP connection is null. @Devs please make sure to hook up the Pi")
      false
    }
    else true
  }

  /**
    * Asks server absolute path to PennSync directory (also requires proper sync dir)
    */
  private def getPennsyncDir(): String = {
    val reqData = RequestDataFactory.create(List(), options.host, 8080, RequestDataFactory.PennsyncDirRequest)
    Await.result(HTTPClientUtils.createRequestAndExecuteRequest(reqData)).contentString
  }

  def mapToServerPending(relPath: String) : String = {
    //TODO: Use nio paths to properly augment
    "pending/" ++ relPath
  }

  // Will autocreate directories in a queried path
  def createNecessaryDirectories(relPath: String) : Unit = {
    val pathAsSections : Vector[String] = relPath.split("/").toVector
    val dirList : Vector[String] = pathAsSections.dropRight(1)

    //If dirList is nonempty take the first term to avoid adding an extraneous "/"
    var currPath = ""
    for(dir <- dirList){
      if(currPath == ""){
        currPath = currPath ++ dir
      }
      else{
        currPath = currPath ++ "/" ++ dir
      }
      if(!ssh.isDirectory(currPath)){
        ssh.mkdir(currPath)
      }
    }
  }

  def sendUntrackRequest(file: MetaFile) : Unit = {
    if(!isConnected()){
      return
    }
    val requestData : RequestData = RequestDataFactory.create(List(file), options.host, 8080, RequestDataFactory.UntrackRequest)
    val result : Future[http.Response] = HTTPClientUtils.createRequestAndExecuteRequest(requestData)
    result.onSuccess(_ => println("Successfully deleted file on the server!"))
  }

  /**
    * Handles tracking of server metafiles
    * @param relPaths paths to send to server
    */
  def trackNewServerFile(relPaths: List[String]) : Unit = {
    if(!isConnected()){
      return
    }
    // Base Directory of Pennsync (default is user.home)
    val serverDir = getPennsyncDir()
    println(s"server dir is: $serverDir")
    // Create dummy metafiles to send in request
    val metaFiles = relPaths.map(relPath => MetaFile(relPath, "", 0))

    val requestData : RequestData = RequestDataFactory.create(metaFiles, options.host, 8080, RequestDataFactory.TrackRequest)
    val result : Future[http.Response] = HTTPClientUtils.createRequestAndExecuteRequest(requestData)

    var serverMetaFiles: List[MetaFile] = List[MetaFile]()
    result.onSuccess { case response =>
      println("received a response from the server")
      serverMetaFiles = LedgerParser.parseJsonString(response.contentString)
      println(s"was serverMetaFiles set correctly?: $serverMetaFiles")
      receiveFiles(serverDir, serverMetaFiles)
    }

  }

  /**
    * Sends a VIEW request to the server and parses content string into a List[MetaFile]
    * @return List[MetaFile]
    */
  def viewServerFiles(clientLedger: ClientLedger) : List[MetaFile] = {
    if(!isConnected()){
      return List[MetaFile]()
    }

    val requestData : RequestData = RequestDataFactory.create(clientLedger.fileMetaData, options.host, 8080, RequestDataFactory.ViewRequest)
    val result : Future[http.Response] = HTTPClientUtils.createRequestAndExecuteRequest(requestData)
    val response = Await.result(result)
    LedgerParser.parseJsonString(response.contentString)
  }

  def pullServerChanges(clientLedger: ClientLedger): Unit = {
    if(!isConnected()) return

    val serverBaseDir = getPennsyncDir()

    val requestData = RequestDataFactory.create(clientLedger.fileMetaData, options.host, 8080, RequestDataFactory.PullRequest)
    val result: Future[http.Response] = HTTPClientUtils.createRequestAndExecuteRequest(requestData)

    var files = List[MetaFile]()
    result.onSuccess { case response =>
      files = LedgerParser.parseJsonString(response.contentString)
      receiveFiles(serverBaseDir, files)
      println("Finished!!")
    }
  }

  def sendFile(metaData: MetaFile, file: java.io.File, reqType: Int) : Unit = {
    if(!isConnected()){
      return
    }
    val requestData : RequestData = RequestDataFactory.create(List(metaData), options.host, 8080, reqType)
    val serverPath = mapToServerPending(metaData.relativePath)
    createNecessaryDirectories(serverPath)

    // Modify to send javafile .toFile
    try{
      sftp.send(file, serverPath)
      val result : Future[http.Response] = HTTPClientUtils.createRequestAndExecuteRequest(requestData)
      result.onSuccess(_ => println("Server received files successfully!"))
      result.onFailure(_ => println("Server did not receive files :("))
    }
    catch {
      //Probably didn't have correct privileges
      case e: SftpException => e.printStackTrace()
    }
  }

  private def receiveFiles(serverBaseDir: String, filePaths: List[MetaFile]): Unit = {
    println("hit receiveFiles!")
    println("pausing watcher:")
    WatchDirScala.pauseWatcher()
    filePaths.foreach { case metaFile =>
      val serverFilePath = Paths.get(serverBaseDir)
        .resolve(Paths.get("pennsync/"))
        .resolve(Paths.get(metaFile.relativePath))
        .toString

      println(s"serverFilePath is: $serverFilePath")


      val clientPathFullPath = syncDir.resolve(Paths.get(metaFile.relativePath))

      sftp.receive(serverFilePath, clientPathFullPath.toString)

      Client.modifyLedgerEntry(metaFile)
    }
    println("Restarting watcher: ")
    WatchDirScala.startWacher()
  }
}
