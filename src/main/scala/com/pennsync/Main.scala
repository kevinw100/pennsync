package com.pennsync

import java.io.File

object Main extends App {
  if (args.length != 2) {
    println("I need a file name and an ip address doofus!")
  } else {
    println(args(0))
    println(args(1))

    val appDirPath = System.getProperty("user.dir")

    val appDir = new File(appDirPath)

    DirList.getFiles(appDir, appDirPath)

  }


}
