# boozle

<!-- prettier-ignore -->
> [!WARNING]
> Work in progress. Not for general consumption. Contains chemicals known to
> the State of California to cause cancer and birth defects or other
> reproductive harm.<sup><a href="https://en.wikipedia.org/wiki/1986_California_Proposition_65">[1]</a></sup>

Write [Discord] bots while leveraging the [Typelevel] ecosystem.

[discord]: https://discord.com
[typelevel]: https://github.com/typelevel

```scala
def smack[F[_]: Interaction] = Cmd(user("target", "who to smack") *: string("reason", "why you're doing it")):
  case (victim, why) =>
    reply(
      s"${victim.getAsMention} ***WHAP***",
      components = List(Button("but why"):
        reply(s"because! $why"))
    )
```
