name := "PennSync"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies += "fr.janalyse" %% "janalyse-ssh" % "0.10.3"


libraryDependencies += "net.liftweb" %% "lift-json" % "3.1.0"

mainClass in assembly := Some("com.pennsync.Main")

