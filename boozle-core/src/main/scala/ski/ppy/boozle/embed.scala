package ski.ppy
package boozle

import mouse.all.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

extension [A](a: A)
  def thread[O](o: Option[O])(f: A => O => A): A = // :thinking:
    o.fold(a)(f(a))

final case class Embed(
  title: Option[String] = None,
  description: Option[String] = None,
) {
  def toJDA: MessageEmbed =
    EmbedBuilder()
      .thread(title)(_.setTitle)
      .thread(description)(_.setDescription)
      .build()
}

// TODO: builder pattern? lenses?
