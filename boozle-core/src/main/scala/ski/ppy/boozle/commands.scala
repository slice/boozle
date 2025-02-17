package ski.ppy
package boozle

import cats.effect.*

trait Cmd[F[_]]:
  type A
  type R <: InteractionResponse[F]
  val args: Args[A]
  def run(as: A)(using Interaction[F], Discord[F], Temporal[F]): F[R]

type CmdBounds[F[_], R] = (Interaction[F], Discord[F], Temporal[F]) ?=> R

object Cmd:
  def withArgs[F[_], CR <: InteractionResponse[F], CA](ca: Args[CA])(
    f: CmdBounds[F, CA => F[CR]],
  ): Cmd[F] =
    new:
      type A = CA
      type R = CR

      // scalafmt: { align.preset = some }
      val args = ca
      def run(as: A)(using Interaction[F], Discord[F], Temporal[F]): F[CR] =
        f(as)

  def apply[
    F[_],
    CR <: InteractionResponse[F],
  ](f: CmdBounds[F, F[CR]])
    : Cmd[F] =
    new:
      type A = Unit
      type R = CR

      // TODO: make method for this:
      val args: Args[A] = Args[A]()(_ => None)
      def run(as: A)(using Interaction[F], Discord[F], Temporal[F]): F[CR] = f
