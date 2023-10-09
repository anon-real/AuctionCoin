ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "AC"
  )

libraryDependencies += "org.ergoplatform" %% "ergo-appkit" % "5.0.3"