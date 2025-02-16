package ski.ppy
package boozle

import cats.*
import cats.syntax.all.*
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.commands.*
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.typelevel.twiddles.*
import org.typelevel.twiddles.syntax.*

import scala.util.Try

enum ParamType(val value: Int):
  case String      extends ParamType(3)
  case Integer     extends ParamType(4)
  case Boolean     extends ParamType(5)
  case User        extends ParamType(6)
  case Channel     extends ParamType(7)
  case Role        extends ParamType(8)
  case Mentionable extends ParamType(9)
  case Number      extends ParamType(10)
  case Attachment  extends ParamType(11)

  def toJDA: OptionType = this match
    case String      => OptionType.STRING
    case Integer     => OptionType.INTEGER
    case Boolean     => OptionType.BOOLEAN
    case User        => OptionType.USER
    case Channel     => OptionType.CHANNEL
    case Role        => OptionType.ROLE
    case Mentionable => OptionType.MENTIONABLE
    case Number      => OptionType.NUMBER
    case Attachment  => OptionType.ATTACHMENT

case class Opt(
  `type`: ParamType,
  name: String,
  description: String,
  required: Boolean = true,
):
  def toJDA: OptionData = OptionData(
    `type`.toJDA,
    name,
    description,
    required,
  )

trait Args[A](val opts: List[Opt]):
  def extract(event: Event.Slash): Option[A]

extension (p: CommandInteractionPayload)
  inline def apply(name: String): Option[OptionMapping] =
    Option(p.getOption(name))

extension (s: Event.Slash)
  def optionString(name: String): Option[String] =
    s.event(name).map(_.getAsString)

  def optionUser(name: String): Option[User] =
    s.event(name).flatMap(m => Try(m.getAsUser).toOption)

  def optionInt(name: String): Option[Int] =
    s.event(name).flatMap(m => Try(m.getAsInt).toOption)

object Args extends TwiddleSyntax[Args]:
  given Applicative[Args]:
    def pure[A](x: A): Args[A] = Args()(_ => x.some)
    def ap[A, B](ff: Args[A => B])(fa: Args[A]): Args[B] =
      Args((ff.opts |+| fa.opts)*): p =>
        for
          a  <- fa.extract(p)
          ff <- ff.extract(p)
        yield ff(a)

  def apply[A](
    opts: Opt*,
  )(f: Event.Slash => Option[A]): Args[A] =
    new Args[A](opts.toList):
      def extract(payload: Event.Slash): Option[A] = f(payload)

  def string(name: String, description: String): Args[String] =
    Args(Opt(ParamType.String, name, description))(_.optionString(name))

  def user(name: String, description: String): Args[User] =
    Args(Opt(ParamType.User, name, description))(_.optionUser(name))

  def int(name: String, description: String): Args[Int] =
    Args(Opt(ParamType.Integer, name, description))(_.optionInt(name))
