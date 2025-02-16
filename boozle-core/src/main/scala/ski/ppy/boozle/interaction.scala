package ski.ppy
package boozle

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*
import fs2.Stream
import mouse.all.*
import net.dv8tion.jda.api.interactions.InteractionHook

enum InteractionResponse:
  case Messaged(hook: InteractionHook)
  case Deferred

trait Interaction[F[_]]:
  def defer(): F[InteractionResponse]

  def reply(
    content: String,
    components: List[Component[F]] = Nil,
  ): F[InteractionResponse]

  def followUp(
    content: String,
    components: List[Component[F]] = Nil,
  ): F[Unit]

object InteractionSummoners:
  def reply[F[_]: Interaction as i](
    content: String,
    components: List[Component[F]] = Nil,
  ): F[InteractionResponse] =
    i.reply(content, components = components)

object Interaction:
  def apply[F[_]](using i: Interaction[F]) = i

  def withEvent[F[_]: {Random, Concurrent}](event: Event)(using
    discord: Discord[F],
  ): Interaction[F] = new:
    override def defer(): F[InteractionResponse] =
      discord.act(event.response.deferReply()).as(InteractionResponse.Deferred)

    private def handleComponentInteractions(
      events: Stream[F, Event],
      components: Map[String, Component[F]],
    ): F[Unit] =
      Stream.emits(components.toVector)
        .covary[F]
        .flatMap { case (id, button: Button[?]) =>
          events.collect:
            case e @ Event.Button(i) if i.getComponentId == id => (button, e)
        }
        .parEvalMapUnbounded { case (button: Button[F], e) =>
          button.click(withEvent[F](e))
        }
        .compile
        .drain

    override def reply(
      content: String,
      components: List[Component[F]] = List.empty,
    ): F[InteractionResponse] =
      for
        components <- components.discernable
        jdaComponents = components.map { case (name, button: Button[?]) =>
          button.toJDA(name)
        }.toList

        // FIXME: this is gross as hell
        action_ = event.response.reply(content)
        action =
          components.isEmpty.fold(action_, action_.addActionRow(jdaComponents*))

        message <- discord.act(action)

        events <- discord.events
        _ <- components.isEmpty.unlessA:
          handleComponentInteractions(
            events.subscribeUnbounded,
            components,
          )
      yield InteractionResponse.Messaged(message)

    override def followUp(
      content: String,
      components: List[Component[F]] = List.empty,
    ): F[Unit] =
      // TODO: clearly unfinished
      discord.act(event.hook.sendMessage(content)).void
