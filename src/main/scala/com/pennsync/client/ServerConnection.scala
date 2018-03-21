package com.pennsync.client

import com.pennsync.MetaFile
import com.twitter.finagle.http
import com.twitter.util.{Await, Future}
import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}
import net.liftweb.json.Formats

object ServerConnection{
  def createConnection(options : SSHOptions)(implicit  formats: Formats) : ServerConnection = {
    new ServerConnection(options)
  }
}

/**
  * Class used to interface with the server
  * @param options
  * @param formats
  */
class ServerConnection(options: SSHOptions)(implicit formats: Formats){
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

  def mapToServerPath(relPath: String) : String = {
    //TODO: Use nio paths to properly augment
    "pending/" ++ relPath
  }

  // Will autocreate directories in a queried path
  def createNecessaryDirectories(relPath: String) : Unit = {
    val pathAsSections : Vector[String] = relPath.split("/").toVector
    val dirList : Vector[String] = pathAsSections.dropRight(1)

    println(s"dirlist: $dirList")
    //If dirList is nonempty take the first term to avoid adding an extraneous "/"
    var currPath = ""
    for(dir <- dirList){
      if(currPath == ""){
        currPath = currPath ++ dir
      }
      else{
        currPath = currPath ++ "/" ++ dir
      }
      println(s"current dir being checked: $currPath")
      if(!ssh.isDirectory(currPath)){
        ssh.mkdir(currPath)
        println(s"Created directory: $currPath")
      }
      else{
        println(s"found a directory: $currPath")
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
    *
    * @param relPaths paths to send to server
    */
  def trackNewServerFile(relPaths: List[String]) : Unit = {
    if(!isConnected()){
      return
    }

    // Create dummy metafiles to send in request
    val metaFiles = relPaths.map(relPath => MetaFile(relPath, "", 0))

    val requestData : RequestData = RequestDataFactory.create(metaFiles, options.host, 8080, RequestDataFactory.TrackRequest)
    val result : Future[http.Response] = HTTPClientUtils.createRequestAndExecuteRequest(requestData)

    var serverMetaFiles: List[MetaFile] = List[MetaFile]()
    result.onSuccess { case response =>
      serverMetaFiles = LedgerParser.parseJsonString(response.contentString)
      receiveFiles(serverMetaFiles)
      println(s"Received serverfiles: $serverMetaFiles")
    }

  }

  /**
    * Sends a VIEW request to the server and parses content string into a List[MetaFile]
    * @return List[MetaFile]
    */
  def viewServerFiles() : List[MetaFile] = {
    if(!isConnected()){
      return List[MetaFile]()
    }

    val requestData : RequestData = RequestDataFactory.create(List(), options.host, 8080, RequestDataFactory.ViewRequest)
    val result : Future[http.Response] = HTTPClientUtils.createRequestAndExecuteRequest(requestData)
    val response = Await.result(result)
    LedgerParser.parseJsonString(response.contentString)
  }

  def pullServerChanges(clientLedger: ClientLedger): Unit = {
    if(!isConnected()) return

    val requestData = RequestDataFactory.create(clientLedger.fileMetaData, options.host, 8080, RequestDataFactory.PullRequest)
    val result: Future[http.Response] = HTTPClientUtils.createRequestAndExecuteRequest(requestData)

    var files = List[MetaFile]()
    result.onSuccess { case response =>
      files = LedgerParser.parseJsonString(response.contentString)
      println(s"files: $files")
      receiveFiles(files)
      println("Finished!!")
    }
  }

  def sendFile(metaData: MetaFile, file: java.io.File, reqType: Int) : Unit = {
    if(!isConnected()){
      return
    }

    val requestData : RequestData = RequestDataFactory.create(List(metaData), options.host, 8080, reqType)

    val serverPath = mapToServerPath(metaData.relativePath)
    createNecessaryDirectories(serverPath)

    // Modify to send javafile .toFile
    sftp.send(file, serverPath)

    val result : Future[http.Response] = HTTPClientUtils.createRequestAndExecuteRequest(requestData)
    result.onSuccess(_ => println("Server received files successfully!"))
    result.onFailure(_ => println("Server did not receive files :("))
  }

  private def receiveFiles(filePaths: List[MetaFile]): Unit = {
    filePaths.foreach { case metaFile =>
      sftp.receive(metaFile.relativePath)
      Client.modifyLedgerEntry(metaFile)
    }
  }
}
