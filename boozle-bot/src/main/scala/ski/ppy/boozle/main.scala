package ski.ppy
package boozle.bot

import cats.*
import cats.effect.*
import cats.effect.std.Random
import cats.syntax.all.*
import fabric.*
import fabric.io.*
import fabric.rw.*
import fs2.Stream
import net.dv8tion.jda.api.entities.User
import ski.ppy.boozle.*
import ski.ppy.boozle.Args.*
import ski.ppy.boozle.InteractionSummoners.*

import java.io.File
import scala.concurrent.duration.*

def smack[F[_]: {Temporal, Interaction, Discord}] = Cmd(
  user("target", "who to smack") *: string("reason", "why you're doing it"),
) { case (victim, why) =>
  val button = Button[F]("but why")
  for
    response <- reply(
      s"${victim.getAsMention} ***WHAP***",
      components = List(button),
    )
    _ <- button.clicks(response)
      .evalMap { case given Interaction[F] => reply("no") }
      .timeout(1.minutes)
      .compile
      .drain
  yield response
}

def commands[F[_]: {Discord, Temporal, Interaction}] =
  Map[String, Cmd[F]]("smack" -> smack)

object Main extends IOApp:
  def config[F[_]](path: String)(using sync: Sync[F]): F[Config] =
    MonadError[F, Throwable].catchNonFatal:
      JsonParser(File(path)).as[Config]

  def app[F[_]: Async](cfg: Config): F[Unit] =
    Discord.fromJDA[F](cfg.token) use: discord =>
      for
        random <- Random.scalaUtilRandom[F]
        given RandomIDs[F] = RandomIDs.fromRandom(random)

        _ <- discord.events
          .debug(event => s"[Debug]: Event: $event")
          .collect:
            case event @ Event.Slash(slash) =>
              given Discord[F]     = discord
              given Interaction[F] = Interaction.withEvent[F](event)
              // TODO: catchNonFatal
              Stream.eval:
                commands[F].get(slash.getName).traverse: cmd =>
                  cmd.run(cmd.args.extract(event).get).void
                .void
          .parJoinUnbounded
          .compile
          .drain
      yield ()

  def run(args: List[String]): IO[ExitCode] =
    for {
      config <- config[IO]("./config.json")
      _      <- app[IO](config)
    } yield ExitCode.Error
