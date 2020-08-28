sbtPlugin := true
conflictManager := ConflictManager.default

name         := "sbt-azureblob-resolver"
organization := "io.github.lukaci"
description  := "SBT plugin which provides Azure BlobStorage bucket resolvers"
version      := "0.10.0"

scalaVersion := "2.12.10"
sbtVersion   := "1.0.2"

libraryDependencies += "com.microsoft.azure" % "azure-storage-blob" % "10.5.0"
