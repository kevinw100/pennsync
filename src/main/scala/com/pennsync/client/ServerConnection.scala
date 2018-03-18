package com.pennsync.client

import com.pennsync.MetaFile
import com.twitter.finagle.http
import com.twitter.util.Future
import fr.janalyse.ssh.{SSH, SSHFtp, SSHOptions}
import net.liftweb.json.Formats

object ServerConnection{
  def createConnection(options : SSHOptions)(implicit  formats: Formats) : ServerConnection = {
    new ServerConnection(options)
  }
}

class ServerConnection(options: SSHOptions)(implicit formats: Formats){
  implicit val ssh: SSH = null //new SSH(options)
  val sftp: SSHFtp = null //new SSHFtp()

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
    * Sends a VIEW request to the server and parses content string into a List[MetaFile]
    * @return List[MetaFile]
    */
  def viewServerFiles() : List[MetaFile] = {
    if(!isConnected()){
      return List[MetaFile]()
    }

    val requestData : RequestData = RequestDataFactory.create(List(), options.host, 8080, RequestDataFactory.ViewRequest)
    val result : Future[http.Response] = HTTPClientUtils.createRequestAndExecuteRequest(requestData)

    //TODO: make this more idiomatic
    var files = List[MetaFile]()
    result.onSuccess(response =>
      files = LedgerParser.parseJsonString(response.contentString)
    )
    files
  }



  def sendFile(metaData: MetaFile, file: java.io.File, reqType: Int) : Unit = {
    if(!isConnected()){
      return
    }
    val requestData : RequestData = RequestDataFactory.create(List(metaData), options.host, 8080, reqType)

    val serverPath = mapToServerPath(metaData.relativePath)
    println(serverPath)

    createNecessaryDirectories(serverPath)
    // Modify to send javafile .toFile
    sftp.send(file, serverPath)

    val result : Future[http.Response] = HTTPClientUtils.createRequestAndExecuteRequest(requestData)
    result.onSuccess(_ => println("Server received files successfully!"))
  }
}
