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
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.IntegrationType

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

import ski.ppy.mootiepop.Args.*

def commands[F[_]](using D: Discord[F]) = Map[String, Cmd[F, ?]](
  "tell" -> Cmd(
    string("target", "who to talk to")
      *: string("message", "what you want to say")
  ) { (target, msg) =>
    D.reply(s"hey $target, someone says ${msg}")
  }
)

object Main extends IOApp {

  val cmds: List[CommandData] = commands(using null).map { (name, cmd) =>
    Commands
      .slash(name, "does :3")
      .addOptions(cmd.args.opts.map(_.toJDA)*)
      .setIntegrationTypes(IntegrationType.ALL)
      .setContexts(InteractionContextType.ALL)
  }.toList

  def run(args: List[String]): IO[ExitCode] = Dispatcher.parallel[IO] use {
    dispatcher =>
      IO.blocking {
        val token =
          "MTMzNzI1MjQzNjE2Njk3MTQ0NA.GocluN.jVoH9j9lnOFB97R2T-xpeuyah7TQcGp1QWaoOo"

        val listener = new ListenerAdapter {
          override def onSlashCommandInteraction(
            event: SlashCommandInteractionEvent
          ): Unit = {
            given Discord[IO] = Discord.ofAsync(event)
            commands[IO].get(event.getName()).foreach { c =>
              dispatcher.unsafeRunAndForget(c.run(c.args.extract(event).get))
            }
          }
        }

        val jda = JDABuilder
          .createLight(token)
          .addEventListeners(listener)
          .build()

        jda.awaitReady()

        jda.updateCommands().addCommands(cmds*).complete()
        println("COMMANDS UPDATED!")

        jda.awaitShutdown()
        ExitCode.Success
      }
  }
}
