inThisBuild(
  List(
    version           := "0.0.0-LOL",
    scalaVersion      := "3.6.2",
    organization      := "ski.ppy",
    startYear         := Some(2025),
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

val V = new {
  def fabric = "1.15.9"
  def fs2    = "3.11.0"
  def cats   = "2.13.0"
}

lazy val mootiepop = (project in file("."))
  .settings(
    name                 := "mootiepop",
    maintainer           := "skippy@ppy.ski",
    Compile / run / fork := true,
    mainClass            := Some("ski.ppy.mootiepop.Main"),
    graalVMNativeImageCommand := s"${sys.env.get("JAVA_HOME").get}/bin/native-image",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "org.typelevel" %% "cats-core"   % V.cats,
      "org.typelevel" %% "cats-free"   % V.cats,
      "org.typelevel" %% "mouse"       % "1.3.2",
      "net.dv8tion" % "JDA" % "5.3.0" exclude ("club.minnced", "opus-java"),
      "org.typelevel" %% "twiddles-core"   % "0.9.0",
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "co.fs2"        %% "fs2-core"        % V.fs2,
      "co.fs2"        %% "fs2-io"          % V.fs2,
      "org.typelevel" %% "fabric-core"     % V.fabric,
      "org.typelevel" %% "fabric-io"       % V.fabric
    )
  )
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(GraalVMNativeImagePlugin)
