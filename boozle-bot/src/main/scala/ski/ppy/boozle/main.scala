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
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import ski.ppy.boozle.*
import ski.ppy.boozle.Args.*
import ski.ppy.boozle.EmbedBuilder.*
import ski.ppy.boozle.InteractionSummoners.*

import java.io.File
import java.util.concurrent.TimeoutException
import scala.concurrent.duration.*
import scala.language.experimental.betterFors

extension (u: User)
  def mention: String = u.getAsMention
  def tag: String     = u.getAsTag

def smack[F[_]] = Cmd.withArgs(
  user("target", "who to smack") *: string("reason", "why you're doing it"),
):
  case (victim, why) =>
    for
      interrogate = Button[F]("but why")

      msg <-
        reply(s"${victim.mention} ***SMACK***", components = List(interrogate))

      _ <- interrogate.clicks(in = msg)
        .onlyFrom(victim).once
        .interact:
          reply(s"${invoker.mention} smacked you because: $why")
        .runFor(1.minute)
    yield msg

def counter[F[_]] = Cmd:
  for
    increment = Button[F]("+1")
    msg <- replyEmbed(embed { title("0") }, components = List(increment))

    _ <- increment.clicks(in = msg)
      .interactTap { deferEdit } // immediately respond to the interaction…
      .buffer(1)
      .zipWithIndex
      .map { case (given Interaction[F], count) =>
        (count, s"$count. Clicked by **${invoker.mention}**!")
      }
      .sliding(3)
      .debounce(3.seconds)       // …but limit updates to a max of once every 3s
      .evalMap { lastThreeClicks =>
        val (latestCount, _) = lastThreeClicks.last.get
        val summary          = lastThreeClicks.map(_._2).mkString_("\n")
        msg.edit(embed { title(s"$latestCount"); description(summary) })
      }
      .runFor(5.minutes)
  yield msg

def commands[F[_]] =
  Map[String, Cmd[F]]("smack" -> smack, "counter" -> counter)

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
        Discord[F].register(commands).whenA(cfg.register) *>
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
