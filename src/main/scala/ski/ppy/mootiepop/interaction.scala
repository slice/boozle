package ski.ppy
package mootiepop

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*
import mouse.all.*
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

  def withEvent[F[_]: {Random, Concurrent}](event: Event)(using
    discord: Discord[F]
  ): Interaction[F] = new:
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
          given Interaction[F] = Interaction.withEvent[F](event)
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
        components <- components.makeDiscernable
        jdaComponents = components.map: (name, component) =>
          // FIXME: hardcoding buttons
          component.asInstanceOf[Button[F]].toJDA(name)
        .toList

        action_ = event.response.reply(content)
        action =
          components.isEmpty.fold(action_, action_.addActionRow(jdaComponents*))

        message <- discord.act(action)

        events <- discord.events
        _ <- components.isEmpty.unlessA:
          handleInteractions(
            events.subscribeUnbounded,
            components
          )
      yield InteractionResponse.Messaged(message)

    override def followUp(
      content: String,
      components: List[Component] = List.empty
    ): F[Unit] =
      discord.act(event.hook.sendMessage(content)).void
