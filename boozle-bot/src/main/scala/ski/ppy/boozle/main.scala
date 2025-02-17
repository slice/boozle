package ski.ppy
package boozle.bot

import cats.*
import cats.effect.*
import cats.effect.std.*
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
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.util.concurrent.TimeoutException

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

    // TODO: make error handling customizable
  def handleCommandError[F[_]: Applicative](
    event: SlashCommandInteractionEvent,
    throwable: Throwable,
  )(using console: Console[F]): F[Unit] =
    console.errorln:
      s"Command ${event.getName} invoked by ${event.getUser.getId} in ${event.getChannel.getId} failed: ${throwable}"
    .whenA(!throwable.isInstanceOf[TimeoutException])

  def handleEvent[F[_]](using
    Temporal[F],
    RandomIDs[F],
    Discord[F],
    Console[F],
  ): PartialFunction[Event, Stream[F, Unit]] =
    case event @ Event.Slash(slash) =>
      given Interaction[F] = Interaction.withEvent[F](event)
      Stream.eval:
        commands[F].get(slash.getName).traverse: cmd =>
          val args = cmd.args.extract(event).get
          cmd.run(args)
            .void
            .handleErrorWith(handleCommandError(slash, _))
        .void

  def app[F[_]: Async](cfg: Config)(using console: Console[F]): F[Unit] =
    Discord.fromJDA[F](cfg.token) use { case (given Discord[F]) =>
      Random.scalaUtilRandom.flatMap: random =>
        given RandomIDs[F] = RandomIDs.fromRandom(random)
        Discord[F].events
          .debug(event => s"[Debug]: Event: $event")
          .collect(handleEvent)
          .parJoinUnbounded
          .compile
          .drain
    }

  def run(args: List[String]): IO[ExitCode] =
    for {
      config <- config[IO]("./config.json")
      _      <- app[IO](config)
    } yield ExitCode.Error
