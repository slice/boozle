package ski.ppy
package boozle

import cats.*
import cats.effect.std.*
import cats.syntax.all.*
import net.dv8tion.jda.api.interactions.components.buttons.Button as JDAButton
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle as JDAButtonStyle

enum ButtonStyle(raw: Int):
  case Primary   extends ButtonStyle(1)
  case Secondary extends ButtonStyle(2)
  case Success   extends ButtonStyle(3)
  case Danger    extends ButtonStyle(4)

  def toJDA: JDAButtonStyle = JDAButtonStyle.fromKey(raw)

sealed trait Component
final case class Button[F[_]](
  val label: String,
  val style: ButtonStyle = ButtonStyle.Secondary
)(val onClick: Interaction[F] ?=> F[InteractionResponse])
    extends Component:
  def toJDA(id: String): JDAButton = JDAButton.of(style.toJDA, id, label)

extension (cs: List[Component])
  def makeDiscernable[F[_]: Monad](using
    random: Random[F]
  ): F[Map[String, Component]] =
    cs.traverse:
      case button: Button[?] =>
        random.nextString(16).map(id => (id, button))
    .map(Map.from)
