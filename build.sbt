
name := "PennSync"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies += "fr.janalyse" %% "janalyse-ssh" % "0.10.3"
libraryDependencies += "net.liftweb" %% "lift-json" % "3.1.0"
libraryDependencies += "com.twitter" %% "finagle-http" % "18.3.0"
libraryDependencies ++= Seq(
  "io.methvin" % "directory-watcher" % "0.4.0",
  "io.methvin" %% "directory-watcher-better-files" % "0.4.0"
)

mainClass in assembly := Some("com.pennsync.client.Main")

fork in run := true
outputStrategy := Some(StdoutOutput)
connectInput in run := true
