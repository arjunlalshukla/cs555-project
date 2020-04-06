name := "p2-identityserver"

version := "0.1"

scalaVersion := "2.13.1"


libraryDependencies += "org.scalactic" %% "scalactic" % "3.1.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % "test"

libraryDependencies += "info.picocli" % "picocli" % "4.2.0"

libraryDependencies += "org.mongodb" % "mongodb-driver-sync" % "4.0.1"
libraryDependencies += "junit" % "junit" % "4.13" % Test
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime

assemblyJarName in assembly := "p2-identityserver.jar"

test in assembly := {}
