package ski.ppy
package mootiepop

import cats.*
import cats.syntax.all.*
import cats.effect.*
import cats.effect.std.*
import scala.concurrent.duration.*

def thonk[F[_]](
    d: Discord[F]
)(using temporal: Temporal[F], console: Console[F]) = for {
  _ <- d.defer()
  _ <- console.println("waiting…")
  _ <- temporal.sleep(3.seconds)
  _ <- d.followUp("hello")
  _ <- temporal.sleep(1.second)
  _ <- d.followUp("you are cool, i think")
} yield ()
