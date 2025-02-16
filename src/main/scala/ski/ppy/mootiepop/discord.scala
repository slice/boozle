package ski.ppy
package mootiepop

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*
import fs2.concurrent.Topic
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.*

trait Discord[F[_]]:
  def events: F[Topic[F, Event]]
  def forget[A](fa: F[A]): F[Unit]
  def register(commands: Map[String, Cmd[F]]): F[Unit]
  def shutdown: F[Unit]

extension [F[_], A](r: Resource.type)
  def delay(a: => A)(using sync: Sync[F]): Resource[F, A] =
    Resource.eval(sync.delay(a))

object Discord:
  private def relayListener[F[_]](
    publish1: Event => Unit
  ): ListenerAdapter = new:
    override def onSlashCommandInteraction(
      e: SlashCommandInteractionEvent
    ): Unit =
      publish1(e.toEvent)

    override def onButtonInteraction(e: ButtonInteractionEvent): Unit =
      publish1(e.toEvent)

  def fromJDA[F[_]](token: String)(using a: Async[F]): Resource[F, Discord[F]] =
    for
      dispatcher <- Dispatcher.parallel[F]
      supervisor <- Supervisor[F](await = false)
      topic      <- Resource.eval(Topic[F, Event])

      jda <- Resource.delay:
        JDABuilder
          .createLight(token)
          .addEventListeners(relayListener: event =>
            dispatcher.unsafeRunAndForget(topic.publish1(event)))
          .build()
    yield new:
      def events: F[Topic[F, Event]]   = topic.pure[F]
      def forget[A](fa: F[A]): F[Unit] = supervisor.supervise(fa).void
      def register(commands: Map[String, Cmd[F]]): F[Unit] =
        val cmds = (commands
          .map: (name, cmd) =>
            net.dv8tion.jda.api.interactions.commands.build.Commands
              .slash(name, "ouch")
              .addOptions(cmd.args.opts.map(_.toJDA)*)
              .setIntegrationTypes(IntegrationType.ALL)
              .setContexts(InteractionContextType.ALL))
          .toList
        jda.updateCommands().addCommands(cmds*).liftAsync.void
      def shutdown: F[Unit] = a.blocking(jda.shutdown())
