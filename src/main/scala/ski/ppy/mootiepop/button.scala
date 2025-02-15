package ski.ppy.mootiepop

import net.dv8tion.jda.api.interactions.components.buttons.{
  Button as JDAButton,
  ButtonStyle as JDAButtonStyle
}

enum ButtonStyle(raw: Int) {
  case Primary   extends ButtonStyle(1)
  case Secondary extends ButtonStyle(2)
  case Success   extends ButtonStyle(3)
  case Danger    extends ButtonStyle(4)

  def toJDA = JDAButtonStyle.fromKey(raw)
}

sealed trait Component
case class Button[F[_]](
  val label: String,
  val style: ButtonStyle = ButtonStyle.Secondary
)(val onClick: Interaction[F] => F[Unit])
    extends Component {
  def toJDA(id: String) = JDAButton.of(style.toJDA, id, label)
}
