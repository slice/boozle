package ski.ppy
package boozle

trait Cmd[F[_]]:
  type A
  val args: Args[A]
  def run(as: A)(using Interaction[F]): F[InteractionResponse[F]]

object Cmd:
  def apply[F[_]: Interaction, CA](ca: Args[CA])(
    f: CA => F[InteractionResponse[F]],
  ): Cmd[F] =
    new:
      type A = CA

      // scalafmt: { align.preset = some }
      val args = ca
      def run(as: A)(using Interaction[F]): F[InteractionResponse[F]] = f(as)
