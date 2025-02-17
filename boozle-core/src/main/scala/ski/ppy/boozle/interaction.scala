package ski.ppy
package boozle

import cats.*
import cats.effect.std.Random
import cats.syntax.all.*
import mouse.all.*
import net.dv8tion.jda.api.interactions.InteractionHook

sealed trait RandomIDs[F[_]]:
  def randomID: F[String]

object RandomIDs:
  def fromRandom[F[_]: Monad](random: Random[F]): RandomIDs[F] = new:
    def randomID: F[String] =
      random.nextPrintableChar.replicateA(16).map(_.mkString)

  extension [F[_]: Applicative](cs: List[Component[F]])
    def discernable(using rids: RandomIDs[F]): F[Map[String, Component[F]]] =
      cs.traverse { case button: Button[?] =>
        rids.randomID.map(id => (id, button))
      }.map(Map.from)

sealed trait InteractionResponse[F[_]]

case class Messaged[F[_]: Monad](
  hook: InteractionHook,
  components: Map[String, Component[F]],
)(using discord: Discord[F]) extends InteractionResponse[F]:
  // TODO: don't return `Unit`
  def edit(content: String): F[Unit] =
    discord.act(hook.editOriginal(content)).void

case class Deferred[F[_]](hook: InteractionHook) extends InteractionResponse[F]

trait Interaction[F[_]] extends RandomIDs[F]:
  def defer: F[Deferred[F]]

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

  def deferEdit[F[_]: InteractionEditable as ie]: F[Deferred[F]] =
    ie.deferEdit

object Interaction:
  def apply[F[_]](using i: Interaction[F]) = i

  def withEvent[F[_]: Monad](event: Event)(using
    discord: Discord[F],
    rids: RandomIDs[F],
  ): Interaction[F] = new:
    export rids.*

    override def defer: F[Deferred[F]] =
      discord.act(event.response.deferReply()).map(Deferred(_))

    override def reply(
      content: String,
      components: List[Component[F]] = List.empty,
    ): F[Messaged[F]] =
      import RandomIDs.*
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
      yield Messaged(message, components)

    override def followUp(
      content: String,
      components: List[Component[F]] = List.empty,
    ): F[Unit] =
      // TODO: clearly unfinished
      discord.act(event.hook.sendMessage(content)).void

trait InteractionEditable[F[_]] extends Interaction[F]:
  def deferEdit: F[Deferred[F]]

object InteractionEditable:
  def withEvent[F[_]](event: Event.Button)(using
    Discord[F],
    RandomIDs[F],
    Monad[F],
  ): InteractionEditable[F] =
    val interaction = Interaction.withEvent[F](event)
    new:
      export interaction.*
      def deferEdit: F[Deferred[F]] =
        Discord[F].act(event.event.deferEdit()).map(Deferred(_))
