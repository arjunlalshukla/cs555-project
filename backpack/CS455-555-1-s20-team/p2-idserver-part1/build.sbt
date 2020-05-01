name := "p2-idserver-part1"

version := "0.1"

scalaVersion := "2.13.1"


libraryDependencies += "org.scalactic" %% "scalactic" % "3.1.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % "test"

libraryDependencies += "info.picocli" % "picocli" % "4.2.0"

libraryDependencies += "org.mongodb" % "mongodb-driver-sync" % "4.0.1"
libraryDependencies += "junit" % "junit" % "4.13" % Test
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"

libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.6.2"

assemblyJarName in assembly := "p2-idserver-part1.jar"

test in assembly := {}
Test / fork := true
