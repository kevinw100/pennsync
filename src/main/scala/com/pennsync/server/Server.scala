package com.pennsync.server

object Server {
  //TODO: overload this for more ways to create a com.pennsync.server (which doesn't require creating a ServerLedger)
  def create(ip: String, ledger: ServerLedger): Server ={
    Server(ip, ledger)
  }
}
case class Server(ip: String, ledger: ServerLedger){
//  def getConflicts(client: Machine, clientLedger: ClientLedger) : Either[]
}

