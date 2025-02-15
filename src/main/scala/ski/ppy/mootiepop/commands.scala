package ski.ppy
package mootiepop

import cats.*
import cats.syntax.all.*
import cats.effect.*
import cats.effect.std.*
import scala.concurrent.duration.*

trait Cmd[F[_], A](val args: Args[A]) {
  def run(i: Interaction[F])(as: A): F[InteractionResponse]
}

object Cmd {
  def apply[F[_], A](
    args: Args[A]
  )(f: (Interaction[F], A) => F[InteractionResponse]): Cmd[F, A] =
    new Cmd[F, A](args) {
      def run(i: Interaction[F])(as: A) = f(i, as)
    }
}
