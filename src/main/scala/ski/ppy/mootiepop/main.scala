package ski.ppy
package mootiepop

import cats.*
import cats.syntax.all.*
import cats.effect.*
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

import net.dv8tion.jda.api.requests.RestAction
import cats.effect.std.Dispatcher
import net.dv8tion.jda.api.entities.Message
import org.typelevel.twiddles.*

extension [F[_]](async: Async[F]) {
  def fromRestAction[A](ra: RestAction[A]): F[A] = async.async_ { cb =>
    ra.queue(a => cb(Right(a)), e => cb(Left(e)))
  }
}

trait Discord[F[_]] {
  def defer(): F[Unit]
  def reply(content: String): F[Unit]
  def followUp(content: String): F[Message]
}

object Discord {
  def ofAsync[F[_]](
      i: SlashCommandInteractionEvent
  )(using F: Async[F]): Discord[F] = new Discord {
    override def defer(): F[Unit] =
      F.fromRestAction(i.deferReply()).void
    override def reply(content: String): F[Unit] =
      F.fromRestAction(i.reply(content)).void
    override def followUp(content: String): F[Message] =
      F.fromRestAction(i.getHook.sendMessage(content))
  }
}

object Main extends IOApp {
  import ski.ppy.mootiepop.Param.*

  def run(args: List[String]): IO[ExitCode] = Dispatcher.parallel[IO] use {
    dispatcher =>
      IO.blocking {
        val token =
          "MTMzNzI1MjQzNjE2Njk3MTQ0NA.GocluN.jVoH9j9lnOFB97R2T-xpeuyah7TQcGp1QWaoOo"

        val listener = new ListenerAdapter {
          override def onSlashCommandInteraction(
              event: SlashCommandInteractionEvent
          ): Unit = {
            event.getOption("hello")
            dispatcher.unsafeRunAndForget(thonk[IO](Discord.ofAsync(event)))
          }
        }

        JDABuilder
          .createLight(token)
          .addEventListeners(listener)
          .build()
          .awaitShutdown()

        ExitCode.Success
      }
  }
}
