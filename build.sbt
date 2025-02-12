name         := "mootiepop"
version      := "0.0.0"
organization := "ski.ppy.mootiepop"
scalaVersion := "3.6.2"

Compile / run / fork := true

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.7",
  "org.typelevel" %% "cats-core"   % "2.13.0",
  "org.typelevel" %% "cats-free"   % "2.13.0",
  "net.dv8tion" % "JDA" % "5.3.0" exclude ("club.minnced", "opus-java")
)

scalacOptions ++= Seq(
  "-old-syntax",
  "-no-indent"
)
