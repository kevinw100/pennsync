package com.pennsync.client

import java.io.{BufferedReader, InputStreamReader}
import java.net.URL

import com.pennsync.MetaFile

import scala.reflect.reify.phases.Metalevels

object Client {
  var clientLedger : ClientLedger = _

  def setClientLedger(ledger: ClientLedger) : Unit = {
    clientLedger = ledger
  }

  def addToLedger(meta: MetaFile) : Unit = {
    clientLedger = clientLedger.addFile(meta)
    clientLedger.write()
  }

  def removeFromLedger(meta: MetaFile) : Unit = {
    clientLedger = clientLedger.deleteFileMetadata(meta)
    clientLedger.write()
  }

  def modifyLedgerEntry(meta: MetaFile) : Unit = {
    clientLedger = clientLedger.updateFileMetadata(meta)
    clientLedger.write()
  }
}
