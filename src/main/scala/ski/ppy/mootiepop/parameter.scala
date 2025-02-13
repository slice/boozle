package ski.ppy
package mootiepop

import cats.*
import cats.syntax.all.*
import org.typelevel.twiddles.*
import org.typelevel.twiddles.syntax.*
import net.dv8tion.jda.api.interactions.commands.*
import scala.util.Try

trait Param[A] {
  def extract(payload: CommandInteractionPayload): Option[A]
}

given Applicative[Param] {
  def pure[A](x: A): Param[A] = Param.fromPayload(_ => x.some)
  def ap[A, B](ff: Param[A => B])(fa: Param[A]): Param[B] =
    Param.fromPayload { p =>
      for {
        a  <- fa.extract(p)
        ff <- ff.extract(p)
      } yield ff(a)
    }
}

object Param extends TwiddleSyntax[Param] {
  def fromPayload[A](f: CommandInteractionPayload => Option[A]): Param[A] =
    new Param {
      def extract(payload: CommandInteractionPayload) = f(payload)
    }

  extension (p: CommandInteractionPayload) {
    inline def apply(name: String): Option[OptionMapping] =
      Option(p.getOption(name))
  }

  def string(name: String): Param[String] =
    fromPayload(_(name).map(_.getAsString))
  def int(name: String): Param[Int] = fromPayload(_(name).flatMap { m =>
    Try(m.getAsInt).toOption
  })
}
