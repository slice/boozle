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
    increment = Button[F]("+1")
    msg <- replyEmbed(embed { title("0") }, components = List(increment))

    _ <- increment.clicks(in = msg)
      .interactTap { deferEdit } // immediately respond to the interaction…
      .buffer(1)
      .zipWithIndex
      .map { case (given Interaction[F], count) =>
        (count, s"$count. Clicked by **${invoker.mention}**!")
      }
      .sliding(3)
      .debounce(3.seconds)       // …but limit updates to a max of once every 3s
      .evalMap { lastThreeClicks =>
        val (latestCount, _) = lastThreeClicks.last.get
        val summary          = lastThreeClicks.map(_._2).mkString_("\n")
        msg.edit(embed { title(s"$latestCount"); description(summary) })
      }
      .runFor(5.minutes)
  yield msg
```
