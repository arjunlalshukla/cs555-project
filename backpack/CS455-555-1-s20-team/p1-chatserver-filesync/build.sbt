name := "p1-chatserver-filesync"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.3",
  "com.lihaoyi" %% "os-lib" % "0.6.2",
  "org.scalatest" % "scalatest_2.13" % "3.1.1" % "test"
)

