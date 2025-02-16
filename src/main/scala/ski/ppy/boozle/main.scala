package ski.ppy
package boozle

import cats.*
import cats.effect.*
import cats.effect.std.Random
import cats.syntax.all.*
import fabric.*
import fabric.io.*
import fabric.rw.*
import fs2.Stream
import net.dv8tion.jda.api.entities.User
import ski.ppy.boozle.Args.*
import InteractionSummoners.*

import java.io.File

def smack[F[_]: Interaction] = Cmd(user(
  "target",
  "who to smack"
) *: string("reason", "why you're doing it")):
  case (victim, why) =>
    reply(
      s"${victim.getAsMention} ***WHAP***",
      components = List(Button("but why"):
        reply(s"because! $why"))
    )

def commands[F[_]: Interaction] = Map[String, Cmd[F]](
  "smack" -> smack
)

object Main extends IOApp:
  def config[F[_]](path: String)(using sync: Sync[F]): F[Config] =
    MonadError[F, Throwable].catchNonFatal:
      JsonParser(File(path)).as[Config]

  def app[F[_]: Async](cfg: Config): F[Unit] =
    Discord.fromJDA[F](cfg.token) use: discord =>
      for
        random <- Random.scalaUtilRandom[F]
        events <- discord.events
        _ <- events.subscribeUnbounded
          .debug(event => s"[Debug]: Event: $event")
          .collect:
            case event @ Event.Slash(slash) =>
              given Discord[F]     = discord
              given Random[F]      = random
              given Interaction[F] = Interaction.withEvent[F](event)
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
