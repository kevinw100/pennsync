package com.pennsync

object Main extends App {
  if (args.length != 2) {
    println("I need a file name and an ip address doofus!")
  } else {
    println(args(0))
    println(args(1))
  }


}
