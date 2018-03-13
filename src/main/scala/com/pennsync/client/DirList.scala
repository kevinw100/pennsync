package com.pennsync.client

import java.io.File
import java.nio.file.Paths
import java.time.{Instant, ZoneOffset, ZonedDateTime, format}

import com.pennsync.MetaFile

object DirList {
  def getUTCTimeString(time : Long): String = {
    val i: Instant = Instant.ofEpochMilli(time)
    val z: ZonedDateTime = ZonedDateTime.ofInstant(i, ZoneOffset.UTC)
    z.format(format.DateTimeFormatter.RFC_1123_DATE_TIME)
  }
  def getFiles(directory: File, appRoot: String, ledgerMap: Map[String, MetaFile]) : Map[String, MetaFile] = {
    if (directory.exists() && directory.isDirectory) {

      var tempMap = ledgerMap

      val subFiles : List[File] = directory.listFiles.filter(_.isFile).toList
      val baseDir = Paths.get(appRoot)

      for (f <- subFiles) {
//        print("Current file being traversed: " + f.toString)

        val fileMetaData = MetaFile.getMetaData(baseDir, f)

        if (tempMap.contains(fileMetaData.relativePath)) {

          tempMap.get(fileMetaData.relativePath) match {
            case Some(oldFileInfo) => if (fileMetaData.lastUpdateLong > oldFileInfo.lastUpdateLong) {
              tempMap = tempMap+(fileMetaData.relativePath -> fileMetaData)
            }
            case None => println("Should never happen")
          }

        } else {
          tempMap = tempMap+(fileMetaData.relativePath -> fileMetaData)
        }

      }

      val subDirectories : List[File] = directory.listFiles.filter(_.isDirectory).toList

      for (d <- subDirectories) {
        tempMap = getFiles(d, appRoot, tempMap)
      }

      tempMap
    } else {
      ledgerMap
    }
  }
}
