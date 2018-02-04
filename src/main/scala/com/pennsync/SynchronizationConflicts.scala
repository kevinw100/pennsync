package com.pennsync

sealed trait SynchronizationConflict{
  def relPath : String
}


case class addedFileOnServer(relPath: String) extends SynchronizationConflict
case class trackedNonExistentFile(relPath: String) extends  SynchronizationConflict

//Occurs when client untracks file not found on server (throw an error)
case class untrackedNonExistentFile(relPath: String) extends SynchronizationConflict

//TODO: Create a conflict for untrack a file when a non-identical file exists on the server (can be implemented after MD5)