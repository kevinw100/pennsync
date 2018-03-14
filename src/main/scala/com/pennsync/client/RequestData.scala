package com.pennsync.client

import com.pennsync.MetaFile


//See https://docs.google.com/document/d/1FTwLN2XzXszcEBl1yA7sgM6EvCgwNQu8Vj84G4hTUQk/edit for more info on these RequestTypes

sealed trait RequestData {
  val data: List[MetaFile]
  val hostname: String
  val port: Int
  val portAsString : String = ":" ++ port.toString
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

case class ViewRequest(val data: List[MetaFile], val hostname: String, val port: Int) extends RequestData {
  val reqType : String = "VIEW"
}

case class UntrackRequest(val data: List[MetaFile], val hostname: String, val port: Int) extends RequestData {
  val reqType : String = "UNTRACK"
}