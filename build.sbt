name         := "mootiepop"
version      := "0.0.0"
organization := "ski.ppy.mootiepop"
scalaVersion := "3.6.2"

Compile / run / fork := true

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.7",
  "org.typelevel" %% "cats-core"   % "2.13.0",
  "org.typelevel" %% "cats-free"   % "2.13.0",
  "org.typelevel" %% "mouse"       % "1.3.2",
  "net.dv8tion" % "JDA" % "5.3.0" exclude ("club.minnced", "opus-java"),
  "org.typelevel" %% "twiddles-core"   % "0.9.0",
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "co.fs2"        %% "fs2-core"        % "3.11.0",
  "co.fs2"        %% "fs2-io"          % "3.11.0"
)

scalacOptions ++= Seq(
  "-old-syntax",
  "-no-indent",
  "-source",
  "future"
)
