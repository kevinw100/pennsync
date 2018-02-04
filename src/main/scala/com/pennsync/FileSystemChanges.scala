package com.pennsync

import java.nio.file.Path

/**
  * Used to make pattern matching easier when looking at changes
  */
sealed trait FileSystemChanges
case class CreateEvent(filePath: Path) extends FileSystemChanges
case class DeleteEvent(filePath: Path) extends FileSystemChanges
case class ModifyEvent(filePath: Path) extends FileSystemChanges
case class TrackEvent(filePath: Path) extends  FileSystemChanges
case class UntrackEvent(filePath: Path) extends  FileSystemChanges