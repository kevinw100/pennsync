package com.pennsync

import java.io.File

object DirList {
  def getFiles(directory : File, appRoot : String) {
    if (directory.exists() && directory.isDirectory()) {
      val subFiles : List[File] = directory.listFiles.filter(_.isFile).toList

      for (f <- subFiles) {
        println(s".${f.getPath.split(appRoot)(1)}")
      }

      val subDirectories : List[File] = directory.listFiles.filter(_.isDirectory).toList

      for (d <- subDirectories) {
        getFiles(d, appRoot)
      }

    }
  }
}
