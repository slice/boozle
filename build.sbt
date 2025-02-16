organization := "ski.ppy.mootiepop"

inThisBuild(
  List(
    scalaVersion      := "3.6.2",
    semanticdbEnabled := true,
    scalafixDependencies ++= Seq(
      "org.typelevel"      %% "typelevel-scalafix" % "0.5.0",
      "com.github.xuwei-k" %% "scalafix-rules"     % "0.6.1"
    ),
    scalacOptions ++= Seq(
      "-new-syntax",
      "-source",
      "future"
    )
  )
)

val fabricVersion = "1.15.9"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.5.7",
  "org.typelevel" %% "cats-core"   % "2.13.0",
  "org.typelevel" %% "cats-free"   % "2.13.0",
  "org.typelevel" %% "mouse"       % "1.3.2",
  "net.dv8tion" % "JDA" % "5.3.0" exclude ("club.minnced", "opus-java"),
  "org.typelevel" %% "twiddles-core"   % "0.9.0",
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "co.fs2"        %% "fs2-core"        % "3.11.0",
  "co.fs2"        %% "fs2-io"          % "3.11.0",
  "org.typelevel" %% "fabric-core"     % fabricVersion,
  "org.typelevel" %% "fabric-io"       % fabricVersion
)

lazy val mootiepop = project
  .in(file("."))
  .settings(
    name                 := "mootiepop",
    version              := "0.0.0",
    maintainer           := "skippy@ppy.ski",
    Compile / run / fork := true,
    mainClass            := Some("ski.ppy.mootiepop.Main"),
    graalVMNativeImageCommand := s"${sys.env.get("JAVA_HOME").get}/bin/native-image"
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(GraalVMNativeImagePlugin)
