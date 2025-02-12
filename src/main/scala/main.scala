package ski.ppy
package mootiepop

import cats.*
import cats.data.*
import cats.syntax.all.*
import cats.effect.*
import cats.data.IndexedStateT.{inspectF, modifyF}
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionHook
import scala.concurrent.duration._

import java.util
import java.util.concurrent.{Callable, ExecutorService, Future, TimeUnit}
import net.dv8tion.jda.api.requests.RestAction
import cats.effect.std.Dispatcher

extension [F[_]: FlatMap, SA, SB, SC, A, B](fa: IndexedStateT[F, SA, SB, A])
  infix def ->(
      fb: => IndexedStateT[F, SB, SC, B]
  ): IndexedStateT[F, SA, SC, B] =
    fa.flatMap(_ => fb)

extension [F[_]](async: Async[F])
  def fromRestAction[A](ra: RestAction[A]): F[A] = async.async_ { cb =>
    ra.queue(a => cb(Right(a)), e => cb(Left(e)))
  }

sealed trait InteractionState
case class Pending(event: SlashCommandInteractionEvent) extends InteractionState
case class Responded(event: SlashCommandInteractionEvent, hook: InteractionHook)
    extends InteractionState

type Unhandled[F[_], A] = StateT[F, Pending, A]
type Responding[F[_], A] = IndexedStateT[F, Pending, Responded, A]
type AlreadyResponded[F[_], A] = StateT[F, Responded, A]

def defer[F[_]: Async as F](ephemeral: Boolean = false): Responding[F, Unit] =
  modifyF:
    case Pending(event) =>
      F.fromRestAction(event.deferReply(ephemeral)).map(Responded(event, _))

def reply[F[_]: Async as F](
    content: String,
    ephemeral: Boolean = true
): Responding[F, Unit] = modifyF:
  case Pending(event) =>
    F.fromRestAction(event.reply(content)).map(Responded(event, _))

def followUp[F[_]: Async as F](content: String): AlreadyResponded[F, Unit] =
  inspectF:
    case Responded(event, hook) =>
      F.fromRestAction(hook.sendMessage(content)).void

def lift[F[_]: Monad, S, A](fa: F[A]): StateT[F, S, A] =
  IndexedStateT.liftF(fa)

def thonk[F[_]: Async] = for
  _ <- defer(ephemeral = true)
  _ <- lift(Temporal[F].sleep(5.seconds))
  _ <- followUp("hello")
  _ <- followUp("hello (2)")
  _ <- followUp("hello (3)")
yield ()

object Main extends IOApp:
  def run(args: List[String]): IO[ExitCode] = IO.blocking:
    val token =
      "MTMzNzI1MjQzNjE2Njk3MTQ0NA.GocluN.jVoH9j9lnOFB97R2T-xpeuyah7TQcGp1QWaoOo"
    JDABuilder
      .createLight(token)
      .addEventListeners(new ListenerAdapter {
        override def onSlashCommandInteraction(
            event: SlashCommandInteractionEvent
        ): Unit =
          Dispatcher.sequential[IO] use: dispatcher =>
            IO.delay(
              dispatcher.unsafeRunAndForget(thonk[IO].run(Pending(event)))
            )
      })
      .build()
    ExitCode.Success
