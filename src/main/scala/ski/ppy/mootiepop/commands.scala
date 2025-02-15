package ski.ppy
package mootiepop

import cats.*
import cats.syntax.all.*
import cats.effect.*
import cats.effect.std.*
import scala.concurrent.duration.*

trait Cmd[F[_]] {
  type A
  val args: Args[A]
  def run(i: Interaction[F])(as: A): F[InteractionResponse]
}

object Cmd {
  def apply[CA](
    as: Args[CA]
  )[F[_]](f: (Interaction[F], CA) => F[InteractionResponse]) = new Cmd[F] {
    type A = CA
    val args = as

    def run(i: Interaction[F])(as: A) = f(i, as)
  }
}
