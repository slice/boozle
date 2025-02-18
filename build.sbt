inThisBuild(
  List(
    version           := "0.0.0-LOL",
    scalaVersion      := "3.6.2",
    organization      := "ski.ppy",
    startYear         := Some(2025),
    semanticdbEnabled := true,
    scalafixDependencies ++= Seq(
      "org.typelevel"      %% "typelevel-scalafix" % "0.5.0",
      "com.github.xuwei-k" %% "scalafix-rules"     % "0.6.1",
    ),
    scalacOptions ++= Seq(
      "-new-syntax",
      "-source",
      "future",
    ),
  ),
)

val V = new {
  def fabric     = "1.15.9"
  def fs2        = "3.11.0"
  def cats       = "2.13.0"
  def catsEffect = "3.5.7"
  def mouse      = "1.3.2"
  def twiddles   = "0.9.0"
  def jda        = "5.3.0"
  def iron       = "2.6.0"
}

lazy val baseDependencies = Seq(
  "org.typelevel"      %% "cats-core"     % V.cats,
  "org.typelevel"      %% "cats-effect"   % V.catsEffect,
  "org.typelevel"      %% "mouse"         % V.mouse,
  "org.typelevel"      %% "twiddles-core" % V.twiddles,
  "co.fs2"             %% "fs2-core"      % V.fs2,
  "co.fs2"             %% "fs2-io"        % V.fs2,
  "io.github.iltotore" %% "iron"          % V.iron,
  "net.dv8tion" % "JDA" % V.jda exclude ("club.minnced", "opus-java"),
)

lazy val root = (project in file("."))
  .aggregate(core, bot)
  .settings(
    maintainer.withRank(KeyRanks.Invisible) := "skip@skip.dog",
  )

lazy val core = (project in file("./boozle-core"))
  .settings(
    name := "boozle-core",
    libraryDependencies ++= baseDependencies,
  )

lazy val bot = (project in file("./boozle-bot"))
  .dependsOn(core)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(GraalVMNativeImagePlugin)
  .settings(
    name                := "boozle-bot",
    run / fork          := true,
    run / baseDirectory := file("."),
    Compile / mainClass := Some("ski.ppy.boozle.bot.Main"),
    graalVMNativeImageCommand := s"${sys.env.get("JAVA_HOME").get}/bin/native-image",
    libraryDependencies ++= baseDependencies,
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "org.typelevel" %% "fabric-core"     % V.fabric,
      "org.typelevel" %% "fabric-io"       % V.fabric,
    ),
  )
