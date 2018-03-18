package com.pennsync.client

import com.pennsync.MetaFile


object RequestDataFactory{

  final val PullRequest = 0
  final val TrackRequest = 1
  final val AddFileRequest = 2
  final val UntrackRequest = 3
  final val ViewRequest = 4

  def create(data: List[MetaFile], hostname: String, port: Int, reqType: Int) : RequestData = {
    reqType match {
      case PullRequest => new PullRequest(data, hostname, port)
      case TrackRequest => new TrackRequest(data, hostname, port)
      case AddFileRequest => new AddFileRequest(data, hostname, port)
      case UntrackRequest => new UntrackRequest(data, hostname, port)
      case ViewRequest => new ViewRequest(data, hostname, port)
    }
  }
}


//See https://docs.google.com/document/d/1FTwLN2XzXszcEBl1yA7sgM6EvCgwNQu8Vj84G4hTUQk/edit for more info on these RequestTypes

sealed trait RequestData {
  val data: List[MetaFile]
  val hostname: String
  val port: Int
  val portAsString : String = port.toString
  val reqType: String
}

case class PullRequest(val data: List[MetaFile], val hostname: String, val port: Int) extends RequestData {
  val reqType : String = "PULL"
}

case class TrackRequest(val data: List[MetaFile], val hostname: String, val port: Int) extends RequestData {
  val reqType : String = "TRACK"
}

case class AddFileRequest(val data: List[MetaFile], val hostname: String, val port: Int) extends RequestData {
  val reqType : String = "ADD_FILE"
}

case class ModifyFileRequest(val data: List[MetaFile], val hostname: String, val port: Int) extends RequestData {
  val reqType : String = "MODIFY_FILE"
}

case class ViewRequest(val data: List[MetaFile], val hostname: String, val port: Int) extends RequestData {
  val reqType : String = "VIEW"
}

case class UntrackRequest(val data: List[MetaFile], val hostname: String, val port: Int) extends RequestData {
  val reqType : String = "UNTRACK"
}