package ski.ppy
package boozle

import cats.*
import fs2.Stream
import fs2.concurrent.Topic
import cats.effect.*
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

type Handler[F[_]] = Interaction[F] ?=> F[InteractionResponse]

sealed trait Component[F[_]]
sealed trait Button[F[_]](
  val label: String,
  val style: ButtonStyle = ButtonStyle.Secondary,
) extends Component[F]:
  def clicks: Stream[F, Interaction[F]]
  def click(i: Interaction[F]): F[Unit]
  def close: F[Unit]
  def toJDA(id: String): JDAButton = JDAButton.of(style.toJDA, id, label)

object Button:
  def apply[F[_]: Concurrent](
    label: String,
    style: ButtonStyle = ButtonStyle.Secondary,
  ): F[Button[F]] =
    for
      topic <- Topic[F, Interaction[F]]
    yield new Button[F](label, style):
      def clicks                   = topic.subscribeUnbounded
      def click(i: Interaction[F]) = topic.publish1(i).void
      def close                    = topic.close.void

extension [F[_]: Applicative](cs: List[Component[F]])
  def discernable(using random: Random[F]): F[Map[String, Component[F]]] =
    cs.traverse:
      case button: Button[?] =>
        random.nextString(16).map(id => (id, button))
    .map(Map.from)
