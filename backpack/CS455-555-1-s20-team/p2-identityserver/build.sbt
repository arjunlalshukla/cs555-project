name := "p2-identityserver"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies += "commons-cli" % "commons-cli" % "1.4"
libraryDependencies += "com.github.losizm" %% "little-cli" % "0.7.0"
libraryDependencies += "org.mongodb" % "mongodb-driver-sync" % "4.0.1"
libraryDependencies += "junit" % "junit" % "4.13" % Test
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime