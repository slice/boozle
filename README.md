# boozle

<!-- prettier-ignore -->
> [!WARNING]
> Work in progress. Not for general consumption. Contains chemicals known to
> the State of California to cause cancer and birth defects or other
> reproductive harm.<sup><a href="https://en.wikipedia.org/wiki/1986_California_Proposition_65">[1]</a></sup>

Write [Discord] bots while leveraging the [Typelevel] ecosystem.

[discord]: https://discord.com
[typelevel]: https://github.com/typelevel

This example sends an embed with a button component that lets users increment
the value displayed. The embed displays who performed the last three increments,
and the updates are debounced to only occur every 3 seconds at most.

```scala
def counter[F[_]] = Cmd:
  for
    inc = Button[F]("+1")
    msg <- replyEmbed(Embed(title = "0".some), components = List(inc))
    _ <- inc.clicks(in = msg)
      .interactTap { deferEdit }
      .buffer(1)
      .zipWithIndex
      .map { case (given Interaction[F], index) =>
        (index, s"$index. Clicked by **${invoker.mention}**!")
      }
      .sliding(3)
      .debounce(3.seconds)
      .evalMap: (chunk) =>
        val latestCount = chunk.last.get._1
        val desc        = chunk.map(_._2).mkString_("\n")
        msg.edit(Embed(title = s"$latestCount".some, description = desc.some))
      .runFor(5.minutes)
  yield msg
```
