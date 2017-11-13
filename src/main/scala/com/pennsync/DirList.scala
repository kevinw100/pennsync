package com.pennsync

import java.io.File
import java.time.{Instant, ZoneOffset, ZonedDateTime, format}

object DirList {
  def getUTCTimeString(time : Long): String = {
    val i: Instant = Instant.ofEpochMilli(time)
    val z: ZonedDateTime = ZonedDateTime.ofInstant(i, ZoneOffset.UTC)
    return z.format(format.DateTimeFormatter.RFC_1123_DATE_TIME)
  }
  def getFiles(directory : File, appRoot : String) {
    if (directory.exists() && directory.isDirectory()) {
      val subFiles : List[File] = directory.listFiles.filter(_.isFile).toList

      for (f <- subFiles) {

        println(s".${f.getPath.split(appRoot)(1)} | Last Modified: ${getUTCTimeString(f.lastModified())}")
      }

      val subDirectories : List[File] = directory.listFiles.filter(_.isDirectory).toList

      for (d <- subDirectories) {
        getFiles(d, appRoot)
      }

    }
  }
}
