package com.pennsync

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path, Paths}

import com.pennsync.Main.{ledgerNewString, newMap}
import net.liftweb.json.{Formats, Serialization, parse}

/**
  * Usage: Parses in a ledger file into a list of metafiles
  */
object LedgerParser {
  /**
    * @param ledgerPath: the relative or absolute path to the ledger.json file
    * @param formats DefaultFormats from the lift framework
    * @return
    */
  def create(ledgerPath: String)(implicit formats: Formats) : LedgerParser = {
    return new LedgerParser(ledgerPath)
  }

}
case class LedgerParser(ledgerPath: String)(implicit formats: Formats){
  /**
    * Reads in the prior ledger file
    * @return
    */
  def fromLedgerFile() : Map[String, MetaFile] = {
    if(!Files.exists(Paths.get(ledgerPath))){
      println(s"[WARN]: No ledger path found at ${ledgerPath}, creating one")
      //Writes an empty list to the file
      writeToFile(List())
    }
    val ledgerString = scala.io.Source.fromFile(ledgerPath).mkString
    val ledgerJson = parse(ledgerString)
    val ledgerList: List[MetaFile] = ledgerJson.extract[List[MetaFile]]
    val ledgerMap = ledgerList.map { case x: MetaFile => (x.relativePath, x) }.toMap
    ledgerMap
  }

  /**
    * Writes out old ledger file
    * @param fileList
    */
  def writeToFile(fileList: List[MetaFile]) : Unit = {
    val ledgerNewString: String = Serialization.write(fileList)
    //TODO: make this with shadowfiles (in case this ledger write fails)
    new PrintWriter(ledgerPath) { write(ledgerNewString); close }
  }

  /**
    * Reads in a FS as a ledger file (less trustworthy than the old ledger)
    * @param syncDir base directory to begin the sync (from main)
    * @return
    */
  def fromFS(syncDir: Path) : Map[String, MetaFile] = {
    DirList.getFiles(new File(syncDir.toString), syncDir.toString, Map())
  }
}