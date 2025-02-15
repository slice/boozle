import scala.reflect.api.TypeTags
import scala.quoted.*
1 + 1

type User = Nothing

type Tagged[N, T] = {
  type Tag0 = N
  type Tag1 = T
}

type Description[D] = { type Desc = D }

type Opt[A, N, T] = A & Tagged[N, T]

def ban(
  target: Opt[User, "target", Description["who to ban"]],
  reason: Opt[String, "reason", Unit]
) = ???
