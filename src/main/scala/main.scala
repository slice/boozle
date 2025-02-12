package ski.ppy
package mootiepop

import cats.*
import cats.data.*
import cats.syntax.all.*
import cats.effect.*
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionHook

import net.dv8tion.jda.api.requests.RestAction
import cats.effect.std.Dispatcher

extension [F[_]: FlatMap, SA, SB, SC, A, B](fa: IndexedStateT[F, SA, SB, A]) {
  infix def ->(
      fb: => IndexedStateT[F, SB, SC, B]
  ): IndexedStateT[F, SA, SC, B] = fa.flatMap(_ => fb)
}

extension [F[_]](async: Async[F]) {
  def fromRestAction[A](ra: RestAction[A]): F[A] = async.async_ { cb =>
    ra.queue(a => cb(Right(a)), e => cb(Left(e)))
  }
}

sealed trait InteractionState
case class Pending(event: SlashCommandInteractionEvent) extends InteractionState
case class Responded(event: SlashCommandInteractionEvent, hook: InteractionHook)
    extends InteractionState

type Unhandled[F[_], A]        = StateT[F, Pending, A]
type Responding[F[_], A]       = IndexedStateT[F, Pending, Responded, A]
type AlreadyResponded[F[_], A] = StateT[F, Responded, A]

trait Discord[F[_]] {
  def defer(): F[Unit]
  def reply(content: String): F[Unit]
  def followUp(content: String): F[Unit]
}

type Interacting[F[_]] = ReaderT[F, SlashCommandInteractionEvent, *]
object Discord {
  import ReaderT.*

  def ofAsync[F[_]](using
      F: Async[F]
  ): Discord[Interacting[F]] = new Discord {
    override def defer(): Interacting[F][Unit] =
      ask flatMapF { e => F.fromRestAction(e.deferReply()).void }
    override def reply(content: String): Interacting[F][Unit] =
      ask flatMapF { e => F.fromRestAction(e.reply(content)).void }
    override def followUp(content: String): Interacting[F][Unit] =
      ask flatMapF { e =>
        F.fromRestAction(e.getHook.sendMessage(content)).void
      }
  }
}

def thonk[F[_]](using Monad[F])(using F: Discord[F]) = for {
  _ <- F.reply("hello")
} yield ()

object Main extends IOApp {
  import cats.effect.unsafe.implicits.global
  def run(args: List[String]): IO[ExitCode] = IO.blocking {
    val token =
      "MTMzNzI1MjQzNjE2Njk3MTQ0NA.GocluN.jVoH9j9lnOFB97R2T-xpeuyah7TQcGp1QWaoOo"
    JDABuilder
      .createLight(token)
      .addEventListeners(new ListenerAdapter {
        override def onSlashCommandInteraction(
            event: SlashCommandInteractionEvent
        ): Unit = {
          given Discord[Interacting[IO]] = Discord.ofAsync
          (Dispatcher.sequential[IO] use { dispatcher =>
            IO.delay(
              dispatcher.unsafeRunAndForget(thonk[Interacting[IO]].run(event))
            )
          }).unsafeRunSync()
        }
      })
      .build()
    ExitCode.Success
  }
}
