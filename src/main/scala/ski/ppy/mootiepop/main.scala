package ski.ppy
package mootiepop

import cats.*
import cats.effect.*
import cats.effect.std.Dispatcher
import cats.effect.std.Random
import cats.effect.std.Supervisor
import fs2.Stream
import fs2.concurrent.Topic
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import ski.ppy.mootiepop.Args.*

def smack[F[_]] = Cmd[F](
  user("target", "who to smack") *: string("reason", "why you're doing it")
) { case (interaction, (victim, why)) =>
  interaction.reply(
    s"${victim.getAsMention} *WHAP*",
    components = List(Button("but why")(_.reply("hoi")))
  )
}

def commands[F[_]] = Map[String, Cmd[F]](
  "smack" -> smack
)

type Event = SlashCommandInteractionEvent | ButtonInteractionEvent

object Main extends IOApp {

  val cmds: List[CommandData] = commands.map { (name, cmd) =>
    Commands
      .slash(name, "ouch")
      .addOptions(cmd.args.opts.map(_.toJDA)*)
      .setIntegrationTypes(IntegrationType.ALL)
      .setContexts(InteractionContextType.ALL)
  }.toList

  def run(args: List[String]): IO[ExitCode] =
    Dispatcher.parallel[IO].both(Supervisor[IO](await = false)) use {
      (dispatcher, supervisor) =>
        for {
          events <- Topic[IO, Event]

          _ <- IO.blocking {
            val token =
              "MTI5ODc4ODk3MzUyNjkxMzA3NA.Gz0d1h.Y-TrM2kl_MTpE6i-hKOEd5Jk2USkhE0WrNMtYA"

            val topicListener = new ListenerAdapter {
              override def onSlashCommandInteraction(
                event: SlashCommandInteractionEvent
              ): Unit = {
                println("Dispatching…")
                dispatcher.unsafeRunAndForget(events.publish1(event))
              }

              override def onButtonInteraction(
                event: ButtonInteractionEvent
              ): Unit =
                dispatcher.unsafeRunAndForget(events.publish1(event))
            }

            JDABuilder
              .createLight(token)
              .addEventListeners(topicListener)
              .build()
          }

          random <- Random.scalaUtilRandom[IO]

          _ <- events.subscribeUnbounded
            .debug { event => s"Incoming event: $event" }
            .collect { case event: SlashCommandInteractionEvent =>
              given Random[IO] = random
              val interaction = Interaction
                .ofAsync[IO](events.subscribeUnbounded, supervisor, event)

              commands[IO]
                .get(event.getName)
                .map { cmd =>
                  cmd.run(interaction)(cmd.args.extract(event).get)
                }
                .getOrElse(IO.unit)
            }
            .parEvalMapUnbounded(identity)
            .compile
            .drain
        } yield ExitCode.Success
    }
}
