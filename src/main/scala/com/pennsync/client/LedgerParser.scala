package com.pennsync.client

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path, Paths}

import com.pennsync.MetaFile
import net.liftweb.json.{Formats, Serialization, parse}

/**
  * Usage: utility functions to parse in ledger files
  */
object LedgerParser {

  /**
    * Reads in a client ledger file
    * @param ledgerPath : Path to the client ledger to write to
    * @return
    */
  def fromClientLedgerFile(ledgerPath: String)(implicit format: Formats) : Map[String, MetaFile] = {
    if(!Files.exists(Paths.get(ledgerPath))){
      println(s"[WARN]: No ledger path found at ${ledgerPath}, creating one")
      //Writes an empty list to the file
      LedgerParser.writeToClientFile(List(), ledgerPath)
    }
    val ledgerString = scala.io.Source.fromFile(ledgerPath).mkString
    val ledgerList: List[MetaFile] = parseJsonString(ledgerString)
    val ledgerMap = ledgerList.map { case x: MetaFile => (x.relativePath, x) }.toMap
    ledgerMap
  }

  def getJsonString(fileList: List[MetaFile])(implicit format: Formats) : String = {
    Serialization.write(fileList)
  }

  def parseJsonString(jsonString: String)(implicit format: Formats) : List[MetaFile] = {
    parse(jsonString).extract[List[MetaFile]]
  }
  /**
    * Writes out old ledger file
    * @param fileList
    */
  def writeToClientFile(fileList: List[MetaFile], ledgerPath: String)(implicit format: Formats) : Unit = {
    val ledgerNewString: String = getJsonString(fileList)
    //TODO: make this with shadowfiles (in case this ledger write fails)
    new PrintWriter(ledgerPath) { write(ledgerNewString); close }
  }

  /**
    * Reads in a FS as a ledger file (less trustworthy than the old ledger)
    * @param syncDir base directory to begin the sync (from main)
    * @return
    */
  def getFilesFromSyncDir(syncDir: Path) : Map[String, MetaFile] = {
    DirList.getFiles(new File(syncDir.toString), syncDir.toString, Map())
  }
}