name := "medium-sdk-scala"

version := "1.0"

description := "Scala SDK for Medium.com"

scalaVersion := "2.11.7"

libraryDependencies += "com.squareup.okhttp3" % "okhttp" % "3.0.1"

libraryDependencies += "io.spray" %% "spray-json" % "1.3.2"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"

libraryDependencies += "com.squareup.okhttp3" % "mockwebserver" % "3.0.1" % "test"