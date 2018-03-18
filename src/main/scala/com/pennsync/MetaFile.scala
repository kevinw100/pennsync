package com.pennsync

import java.nio.file.{Path, Paths}

import com.pennsync.client.DirList

object MetaFile {
  def getMetaData(baseDir: Path, file: java.io.File) : MetaFile = {
    val child = Paths.get(file.toString).toAbsolutePath
    val childFile : java.io.File = new java.io.File(child.toString)
    val relPath = baseDir.relativize(child)
    val lastModifiedLong = childFile.lastModified()
    val lastModified = DirList.getUTCTimeString(lastModifiedLong)
    MetaFile(relPath.toString, lastModified, lastModifiedLong)
  }
}
case class MetaFile (relativePath: String, lastUpdateString: String, lastUpdateLong: Long){
  //For pretty-printing
  override def toString: String = s"relativePath: $relativePath\nlastUpdateString: $lastUpdateString\nlastUpdateLong: $lastUpdateLong\n"
}
