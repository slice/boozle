package ski.ppy
package mootiepop

import cats.*
import cats.syntax.all.*
import cats.effect.*
import mouse.all.*
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
import net.dv8tion.jda.api.interactions.InteractionHook
import fs2.Stream
import fs2.concurrent.Topic
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.{
  ActionComponent as JDAComponent
}
import cats.effect.std.Random
import cats.effect.std.Supervisor
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.interactions.components.ActionRow

extension [F[_]](async: Async[F]) {
  def fromRestAction[A](ra: RestAction[A]): F[A] = async.async_ { cb =>
    ra.queue(a => cb(Right(a)), e => cb(Left(e)))
  }
}

// the my first approach would be to have your response method be F[InteractionModal[F]] or something like
// trait InteractionModal[F[_]] {
//   def interaction: F[Interaction]
// probably backed by a Deferred that's handled in the event handler or somewhere

// not actually tahthat??
enum InteractionResponse {
  case Messaged(hook: InteractionHook)
  case Deferred
}

trait Interaction[F[_]] {
  def defer(): F[InteractionResponse]

  def reply(
    content: String,
    components: List[Component] = List.empty
  ): F[InteractionResponse]

  def followUp(
    content: String,
    components: List[Component] = List.empty
  ): F[Unit]
}

extension (cs: List[Component]) {
  def makeDiscernable[F[_]: Monad](using
    random: Random[F]
  ): F[List[(String, Component, JDAComponent)]] =
    cs.traverse { case button: Button[F] =>
      random.nextString(16).map { id => (id, button, button.toJDA(id)) }
    }
}

object Interaction {
  def ofAsync[F[_]](
    events: Stream[F, Event],
    supervisor: Supervisor[F],
    event: Event
  )(using F: Async[F], random: Random[F]): Interaction[F] = new Interaction {
    override def defer(): F[InteractionResponse] =
      F.fromRestAction(event.deferReply()).as(InteractionResponse.Deferred)

    override def reply(
      content: String,
      components: List[Component] = List.empty
    ): F[InteractionResponse] = for {
      action <- event.reply(content).pure[F]

      components <- components.makeDiscernable
      _ <-
        F.delay(action.addActionRow(components.map(_._3)*))
          .whenA(!components.isEmpty)

      message <- F.fromRestAction(action)

      _ <- supervisor.supervise(
        events
          .collect { case e: ButtonInteractionEvent => e }
          .map { event =>
            val component = components
              .find(_._1 == event.getComponentId)
              .map(_._2.asInstanceOf[Button[F]])
            component.map((event, _))
          }
          .unNone
          .evalMap { (event, button) =>
            button.onClick(Interaction.ofAsync[F](events, supervisor, event))
          }
          .compile
          .drain
      )
    } yield InteractionResponse.Messaged(message)

    override def followUp(
      content: String,
      components: List[Component] = List.empty
    ): F[Unit] =
      F.fromRestAction(event.getHook.sendMessage(content)).void
  }
}

import ski.ppy.mootiepop.Args.*

def bold(text: String): String = s"**$text**"

def smack[F[_]] = Cmd(
  user("target", "who to smack") *: string("reason", "why you're doing it")
)[F] { case (interaction, (victim, why)) =>
  interaction.reply(
    s"${victim.getAsMention} *WHAP*",
    components = List(Button("but why?") { _.reply(s"because: $why") })
  )
}

def commands[F[_]] = Map[String, Cmd[F]](
  "smack" -> smack
)

type Event = SlashCommandInteractionEvent | ButtonInteractionEvent

object Main extends IOApp {

  val cmds: List[CommandData] = commands.map { (name, cmd) =>
    show("hello thereeee")
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
