package ski.ppy
package boozle

import cats.*
import fs2.Stream
import net.dv8tion.jda.api.interactions.components.buttons.Button as JDAButton
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle as JDAButtonStyle

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
  infix def clicks(in: Messaged[F]): Stream[F, InteractionEditable[F]]
  def toJDA(id: String): JDAButton = JDAButton.of(style.toJDA, id, label)

object Button:
  def apply[F[_]: {Monad, RandomIDs as rids, Discord as discord}](
    label: String,
    style: ButtonStyle = ButtonStyle.Secondary,
  ): Button[F] =
    new Button[F](label, style) { button =>
      def clicks(messaged: Messaged[F]): Stream[F, InteractionEditable[F]] =
        discord.events
          .only[Event.Button]
          .withFilter { event =>
            val pressed = messaged.components.get(event.componentId)
            // XXX: reference equality
            pressed.map(_ eq button).getOrElse(false)
          }
          .map(InteractionEditable.withEvent[F](_))
    }
