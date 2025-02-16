package ski.ppy
package mootiepop

import cats.*
import cats.effect.*
import cats.effect.std.Random
import cats.syntax.all.*
import fs2.Stream
import net.dv8tion.jda.api.entities.User
import ski.ppy.mootiepop.Args.*

def smack[F[_]: Interaction] = Cmd(
  user("target", "who to smack") *: string("reason", "why you're doing it")
):
  case (victim, why) =>
    Interaction[F].reply(
      s"${victim.getAsMention} *WHAP*",
      components = List(Button("but why"):
        Interaction[F].reply(s"because: $why"))
    )

def commands[F[_]: Interaction] = Map[String, Cmd[F]](
  "smack" -> smack
)

object Main extends IOApp:
  def app[F[_]](token: String)(using Async[F]): F[ExitCode] =
    Discord.fromJDA[F](token).use: discord =>
      for {
        random <- Random.scalaUtilRandom[F]
        events <- discord.events
        _ <- events.subscribeUnbounded
          .debug(event => s"[Debug]: Event: $event")
          .collect:
            case event @ Event.Slash(slash) =>
              given Discord[F]     = discord
              given Random[F]      = random
              given Interaction[F] = Interaction.ofAsync[F](event)
              Stream.eval:
                commands[F].get(slash.getName).traverse: cmd =>
                  cmd.run(cmd.args.extract(event).get).void
                .void
          .parJoinUnbounded
          .compile
          .drain
      } yield ExitCode.Success

  def run(args: List[String]): IO[ExitCode] =
    app[IO](
      "MTI5ODc4ODk3MzUyNjkxMzA3NA.Gz0d1h.Y-TrM2kl_MTpE6i-hKOEd5Jk2USkhE0WrNMtYA"
    )
