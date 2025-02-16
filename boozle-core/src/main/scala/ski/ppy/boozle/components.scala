package ski.ppy
package boozle

import cats.*
import cats.effect.std.Random
import cats.syntax.all.*
import fs2.Stream
import net.dv8tion.jda.api.interactions.components.buttons.Button as JDAButton
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle as JDAButtonStyle
import ski.ppy.boozle.InteractionResponse.*

sealed trait RandomIDs[F[_]]:
  def gen: F[String]

object RandomIDs:
  def fromRandom[F[_]: Monad](random: Random[F]): RandomIDs[F] = new:
    def gen: F[String] =
      random.nextPrintableChar.replicateA(16).map(_.mkString)

enum ButtonStyle(raw: Int):
  case Primary   extends ButtonStyle(1)
  case Secondary extends ButtonStyle(2)
  case Success   extends ButtonStyle(3)
  case Danger    extends ButtonStyle(4)

  def toJDA: JDAButtonStyle = JDAButtonStyle.fromKey(raw)

type Handler[F[_]] = Interaction[F] ?=> F[InteractionResponse[F]]

sealed trait Component[F[_]]
sealed trait Button[F[_]](
  val label: String,
  val style: ButtonStyle = ButtonStyle.Secondary,
) extends Component[F]:
  def clicks(container: Messaged[F]): Stream[F, Interaction[F]]
  def toJDA(id: String): JDAButton = JDAButton.of(style.toJDA, id, label)

object Button:
  def apply[F[_]: {Monad, RandomIDs as rids, Discord as discord}](
    label: String,
    style: ButtonStyle = ButtonStyle.Secondary,
  ): Button[F] =
    new Button[F](label, style) { button =>
      def clicks(messaged: Messaged[F]): Stream[F, Interaction[F]] =
        discord.events
          .only[Event.Button]
          .withFilter { event =>
            val pressed = messaged.components.get(event.componentId)
            // XXX: reference equality
            pressed.map(_ eq button).getOrElse(false)
          }
          .map(Interaction.withEvent[F](_))
    }

extension [F[_]: Applicative](cs: List[Component[F]])
  def discernable(using rids: RandomIDs[F]): F[Map[String, Component[F]]] =
    cs.traverse { case button: Button[?] => rids.gen.map(id => (id, button)) }
      .map(Map.from)
