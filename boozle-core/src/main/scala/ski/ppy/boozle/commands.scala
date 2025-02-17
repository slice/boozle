package ski.ppy
package boozle

trait Cmd[F[_]]:
  type A
  type R <: InteractionResponse[F]
  val args: Args[A]
  def run(as: A)(using Interaction[F]): F[R]

object Cmd:
  def apply[F[_], CR <: InteractionResponse[F], CA](ca: Args[CA])(
    f: Interaction[F] ?=> CA => F[CR],
  ): Cmd[F] =
    new:
      type A = CA
      type R = CR

      // scalafmt: { align.preset = some }
      val args = ca
      def run(as: A)(using Interaction[F]): F[CR] = f(as)

  def apply[
    F[_],
    CR <: InteractionResponse[F],
  ](f: Interaction[F] ?=> F[CR])
    : Cmd[F] =
    new:
      type A = Unit
      type R = CR

      // TODO: make method for this:
      val args = Args[A]()(_ => None)
      def run(as: A)(using Interaction[F]): F[CR] = f
