package ski.ppy
package mootiepop

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*
import fs2.Stream
import net.dv8tion.jda.api.interactions.InteractionHook

enum InteractionResponse:
  case Messaged(hook: InteractionHook)
  case Deferred

trait Interaction[F[_]]:
  def defer(): F[InteractionResponse]

  def reply(
    content: String,
    components: List[Component] = Nil
  ): F[InteractionResponse]

  def followUp(
    content: String,
    components: List[Component] = Nil
  ): F[Unit]

object Interaction:
  def apply[F[_]](using i: Interaction[F]) = i

  def ofAsync[F[_]](event: Event)(using
    async: Async[F],
    discord: Discord[F]
  )(using Random[F]): Interaction[F] = new:
    override def defer(): F[InteractionResponse] =
      discord.act(event.response.deferReply()).as(InteractionResponse.Deferred)

    private def handleInteractions(
      events: Stream[F, Event],
      components: Map[String, Component]
    ): F[Unit] =
      events.collect:
        case e: Event.Button => e
      .map: event =>
        for
          (_, component) <-
            components.find((name, _) => name == event.componentId)
          // FIXME: hardcoding buttons
          button = component.asInstanceOf[Button[F]]
        yield Stream.eval:
          given Interaction[F] = Interaction.ofAsync[F](event)
          button.onClick
      .unNone
        .parJoinUnbounded
        .compile
        .drain

    override def reply(
      content: String,
      components: List[Component] = List.empty
    ): F[InteractionResponse] =
      for
        action <- event.response.reply(content).pure[F]

        components <- components.makeDiscernable
        jdaComponents = components.map: (name, component) =>
          // FIXME: hardcoding buttons
          component.asInstanceOf[Button[F]].toJDA(name)
        .toList

        hasComponents = !components.isEmpty
        _ <- async
          .delay(action.addActionRow(jdaComponents*))
          .whenA(hasComponents)

        message <- discord.act(action)

        events <- discord.events
        _ <- discord.forget(handleInteractions(
          events.subscribeUnbounded,
          components
        )).whenA(hasComponents)
      yield InteractionResponse.Messaged(message)

    override def followUp(
      content: String,
      components: List[Component] = List.empty
    ): F[Unit] =
      discord.act(event.hook.sendMessage(content)).void
