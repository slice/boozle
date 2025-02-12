package ski.ppy
package mootiepop

import cats.*
import cats.syntax.all.*
import cats.effect.*
import cats.effect.std.*
import scala.concurrent.duration.*

def thonk[F[_]](
    discord: Discord[F]
)(using async: Async[F], console: Console[F]) = for {
  _ <- discord.defer()
  _ <- console.println("waiting…")
  _ <- async.sleep(3.seconds)
  _ <- discord.followUp("hello")
} yield ()
