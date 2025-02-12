ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.2"

Compile / run / fork := true

lazy val root = (project in file("."))
  .settings(
    name := "mootiepop3",
    idePackagePrefix := Some("ski.ppy.mootiepop")
  )

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.7",
  "org.typelevel" %% "cats-core" % "2.13.0",
  "org.typelevel" %% "cats-free" % "2.13.0",
  "net.dv8tion" % "JDA" % "5.3.0" exclude ("club.minnced", "opus-java")
)
