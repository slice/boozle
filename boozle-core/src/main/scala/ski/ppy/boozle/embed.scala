package ski.ppy
package boozle

import mouse.all.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

private object WeaveOps:
  extension [A](a: A)
    def weave[O](o: Option[O])(f: A => O => A): A = // :thinking:
      o.fold(a)(f(a))

final case class Embed(
  title: Option[String] = None,
  description: Option[String] = None,
) {
  import WeaveOps.*

  def toJDA: MessageEmbed =
    EmbedBuilder()
      .weave(title)(_.setTitle)
      .weave(description)(_.setDescription)
      .build()
}

// TODO: builder pattern? lenses?
