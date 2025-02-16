package ski.ppy
package mootiepop

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*
import fs2.Stream
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.RestAction

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

extension [F[_]](async: Async[F])
  def fromRestAction[A](ra: RestAction[A]): F[A] = async.`async_`: cb =>
    ra.queue(a => cb(Right(a)), e => cb(Left(e)))

object Interaction:
  def ofAsync[F[_]](
    events: Stream[F, Event],
    supervisor: Supervisor[F],
    event: Event
  )(using F: Async[F], random: Random[F]): Interaction[F] = new Interaction:
    override def defer(): F[InteractionResponse] =
      F.fromRestAction(event.deferReply()).as(InteractionResponse.Deferred)

    override def reply(
      content: String,
      components: List[Component] = List.empty
    ): F[InteractionResponse] = for
      action <- event.reply(content).pure[F]

      components <- components.makeDiscernable
      _ <- F
        .delay(action.addActionRow(components.map(_._3)*))
        .whenA(!components.isEmpty)

      message <- F.fromRestAction(action)

      _ <- supervisor.supervise:
        events
          .collect:
            case e: ButtonInteractionEvent => e
          .map: event =>
            val component = components
              .find(_._1 == event.getComponentId)
              .map(_._2.asInstanceOf[Button[F]])
            component.map((event, _))
          .unNone
          .evalMap: (event, button) =>
            button.onClick(Interaction.ofAsync[F](events, supervisor, event))
          .compile
          .drain
    yield InteractionResponse.Messaged(message)

    override def followUp(
      content: String,
      components: List[Component] = List.empty
    ): F[Unit] =
      F.fromRestAction(event.getHook.sendMessage(content)).void
