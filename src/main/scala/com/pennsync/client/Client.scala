package com.pennsync.client

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL

import com.pennsync.MetaFile

import scala.reflect.reify.phases.Metalevels

object Client {
  val ip : String = ipAddress()
  val id : Int = getUniqueId()
  var clientLedger : ClientLedger = _

  def setClientLedger(ledger: ClientLedger) : Unit = {
    clientLedger = ledger
  }


  def addToLedger(meta: MetaFile) : Unit = {
    clientLedger = clientLedger.addFile(meta)
  }

  def removeFromLedger(meta: MetaFile) : Unit = {
    clientLedger = clientLedger.deleteFileMetadata(meta)
  }

  def modifyLedgerEntry(meta: MetaFile) : Unit = {
    clientLedger = clientLedger.updateFileMetadata(meta)
  }

  def ipAddress(): String = {
    val whatismyip = new URL("http://checkip.amazonaws.com")

    // From https://stackoverflow.com/questions/38392549/get-public-ip-address-of-the-current-machine-using-scala
    val in:BufferedReader = new BufferedReader(
      new InputStreamReader(
        whatismyip.openStream()
      )
    )
    in.readLine()
  }

  def getUniqueId() : Int = {
    //TODO: Figure out a way to get a unique ID/IP address from the Server and save it
    0
  }

}
