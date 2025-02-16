name         := "mootiepop"
version      := "0.0.0"
organization := "ski.ppy.mootiepop"

inThisBuild(
  List(
    scalaVersion      := "3.6.2",
    semanticdbEnabled := true,
    scalafixDependencies ++= Seq(
      "org.typelevel"      %% "typelevel-scalafix" % "0.5.0",
      "com.github.xuwei-k" %% "scalafix-rules"     % "0.6.1"
    )
  )
)

Compile / run / fork := true

object V {
  val cats       = "2.13.0"
  val catsEffect = "3.5.7"
  val fs2        = "3.11.0"
}

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % V.catsEffect,
  "org.typelevel" %% "cats-core"   % V.cats,
  "org.typelevel" %% "cats-free"   % V.cats,
  "org.typelevel" %% "mouse"       % "1.3.2",
  "net.dv8tion" % "JDA" % "5.3.0" exclude ("club.minnced", "opus-java"),
  "org.typelevel" %% "twiddles-core"   % "0.9.0",
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "co.fs2"        %% "fs2-core"        % V.fs2,
  "co.fs2"        %% "fs2-io"          % V.fs2
)

scalacOptions ++= Seq(
  "-old-syntax",
  "-no-indent",
  "-source",
  "future"
)
