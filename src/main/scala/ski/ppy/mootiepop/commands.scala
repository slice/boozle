package ski.ppy
package mootiepop

trait Cmd[F[_]]:
  type A
  val args: Args[A]
  def run(i: Interaction[F])(as: A): F[InteractionResponse]

object Cmd:
  def apply[F[_]]: [CA] => (as: Args[CA]) => (
    (Interaction[F], CA) => F[InteractionResponse]
  ) => Cmd[F] = [CA] =>
    (as: Args[CA]) =>
      (f: (Interaction[F], CA) => F[InteractionResponse]) =>
        new Cmd[F]:
          type A = CA
          val args = as

          def run(i: Interaction[F])(as: A): F[InteractionResponse] = f(i, as)
