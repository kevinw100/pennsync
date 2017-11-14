package com.pennsync

import java.io.File
import java.time.{Instant, ZoneOffset, ZonedDateTime, format}

object DirList {
  def getUTCTimeString(time : Long): String = {
    val i: Instant = Instant.ofEpochMilli(time)
    val z: ZonedDateTime = ZonedDateTime.ofInstant(i, ZoneOffset.UTC)
    return z.format(format.DateTimeFormatter.RFC_1123_DATE_TIME)
  }
  def getFiles(directory: File, appRoot: String, ledgerMap: Map[String, MetaFile]) : Map[String, MetaFile] = {
    if (directory.exists() && directory.isDirectory()) {

      var tempMap = ledgerMap

      val subFiles : List[File] = directory.listFiles.filter(_.isFile).toList

      for (f <- subFiles) {
        val relativePath = f.getPath.split(appRoot)(1)
        val lastModified = f.lastModified()

        if (tempMap.contains(relativePath)) {

          tempMap.get(relativePath) match {
            case Some(oldFileInfo) => if (lastModified > oldFileInfo.lastUpdateLong) {
              tempMap = tempMap+(relativePath -> MetaFile(relativePath, getUTCTimeString(lastModified), lastModified))
            }
            case None => println("Should never happen")
          }

        } else {
          tempMap = tempMap+(relativePath -> MetaFile(relativePath, getUTCTimeString(lastModified), lastModified))
        }

      }

      val subDirectories : List[File] = directory.listFiles.filter(_.isDirectory).toList

      for (d <- subDirectories) {
        tempMap = getFiles(d, appRoot, tempMap)
      }

      return tempMap
    } else {
      return ledgerMap
    }
  }
}
