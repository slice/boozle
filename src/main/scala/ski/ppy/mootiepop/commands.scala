package ski.ppy
package mootiepop

trait Cmd[F[_]]:
  type A
  val args: Args[A]
  def run(as: A)(using Interaction[F]): F[InteractionResponse]

object Cmd:
  def apply[F[_]: Interaction, CA](ca: Args[CA])(
    f: CA => F[InteractionResponse]
  ): Cmd[F] =
    new:
      type A = CA

      val args = ca

      def run(as: A)(using Interaction[F]): F[InteractionResponse] = f(as)
