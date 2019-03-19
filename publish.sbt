organization := "info.lukaci"
organizationName := "lukaci"
organizationHomepage := Some(url("https://github.com/lukaci"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/lukaci/sbt-azureblob-resolver"),
        "scm:git@github.com:lukaci/sbt-azureblob-resolver.git"
  )
)

developers := List(
  Developer(
    id    = "lukaci",
    name  = "lukaci",
    email = "lukaci@gmail.com",
    url   = url("https://github.com/lukaci")
  )
)

description := "Plugin to ease resolving dependencies from and publish to Azure BlobStorage containers, using custom url syntax blob:// (default)."
licenses := List("GNU General Public License, Version 3.0" -> new URL("https://www.gnu.org/licenses/gpl.txt"))
homepage := Some(url("https://github.com/lukaci/sbt-azureblob-resolver"))

pomIncludeRepository := { _ => false }
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
publishMavenStyle := true