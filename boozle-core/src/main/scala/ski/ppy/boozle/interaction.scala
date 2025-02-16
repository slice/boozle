package ski.ppy
package boozle

import cats.*
import cats.syntax.all.*
import mouse.all.*
import net.dv8tion.jda.api.interactions.InteractionHook

import InteractionResponse.*

enum InteractionResponse[F[_]]:
  case Messaged(hook: InteractionHook, components: Map[String, Component[F]])
  case Deferred(hook: InteractionHook)

trait Interaction[F[_]]:
  def defer(): F[Deferred[F]]

  def reply(
    content: String,
    components: List[Component[F]] = Nil,
  ): F[Messaged[F]]

  def followUp(
    content: String,
    components: List[Component[F]] = Nil,
  ): F[Unit]

object InteractionSummoners:
  def reply[F[_]: Interaction as i](
    content: String,
    components: List[Component[F]] = Nil,
  ): F[Messaged[F]] =
    i.reply(content, components = components)

object Interaction:
  def apply[F[_]](using i: Interaction[F]) = i

  def withEvent[F[_]: {RandomIDs, Monad}](event: Event)(using
    discord: Discord[F],
  ): Interaction[F] = new:
    override def defer(): F[Deferred[F]] =
      discord.act(event.response.deferReply()).map: hook =>
        Deferred(hook)

    override def reply(
      content: String,
      components: List[Component[F]] = List.empty,
    ): F[Messaged[F]] =
      for
        components <- components.discernable
        jdaComponents = components.map { case (name, button: Button[?]) =>
          button.toJDA(name)
        }.toVector

        action = event.response.reply(content)
          .thrush: rca =>
            if !components.isEmpty then rca.addActionRow(jdaComponents*)
            else rca

        message <- discord.act(action)
      yield InteractionResponse.Messaged(message, components)

    override def followUp(
      content: String,
      components: List[Component[F]] = List.empty,
    ): F[Unit] =
      // TODO: clearly unfinished
      discord.act(event.hook.sendMessage(content)).void
