package com.pennsync.client

import com.pennsync.MetaFile
import com.twitter.finagle.{Http, Service, ServiceFactory, http}
import com.twitter.util.{Await, Future}
import net.liftweb.json.{Formats, Serialization, parse}
import com.twitter.conversions.time._

object HTTPClientUtils {

  def createRequestAndExecuteRequest(reqData: RequestData)(implicit formats: Formats) : Future[http.Response] = {
    //May have to tune these parameters
    print(s"sending data to ${reqData.hostname ++ ":" ++ reqData.portAsString}")
    val client: Service[http.Request, http.Response] =
      Http.client
        .withSession
        .acquisitionTimeout(5.seconds)
        .newService(reqData.hostname ++ ":" ++ reqData.portAsString)

    val postRequest = http.Request(http.Method.Post, "/")
    val getRequest = http.Request(http.Method.Get, "/")
    //Issues a post request (may have to change with the VIEW and
    val request = reqData match{
      case _ : TrackRequest => getRequest
      case _ : ViewRequest => getRequest
      case _ : AddFileRequest => postRequest
      case _ : ModifyFileRequest => postRequest
      case _ : UntrackRequest => postRequest
      case _ : PullRequest => getRequest
    }
    request.charset_=("UTF_8")
    request.contentString_=(serializeRequest(reqData))
    request.host = reqData.hostname
    val response : Future[http.Response] = client(request)
    response
  }

  private def serializeRequest(reqData: RequestData)(implicit formats: Formats) : String = {
    val serializedString : String = Serialization.write(HTTPRequestWrapper(reqData.data, reqData.reqType))
    serializedString
  }

  /**
    * Wraps RequestData so it omits host + port info
    */
  case class HTTPRequestWrapper(data: List[MetaFile], reqType: String)

}
