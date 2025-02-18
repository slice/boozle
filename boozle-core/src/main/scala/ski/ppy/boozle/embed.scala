package ski.ppy
package boozle

import cats.syntax.all.*
import mouse.all.*
import net.dv8tion.jda.api.EmbedBuilder as JDAEmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

import java.net.URL

private object WeaveOps:
  extension [A](a: A)
    def weave[O](o: Option[O])(f: (A, O) => A): A = // :thinking:
      o.fold(a) { oo => f(a, oo) }

  extension [A](oa: Option[A])
    def toStringOrNull: String | Null =
      oa.map(_.toString).orNull()

final case class Embed(
  title: Option[String] = None,
  description: Option[String] = None,
  author: Option[Embed.Author] = None,
) {
  import WeaveOps.*

  private[boozle] def toJDA: MessageEmbed =
    JDAEmbedBuilder()
      .weave(title)(_.setTitle(_))
      .weave(description)(_.setDescription(_))
      .weave(author) { case (builder, Embed.Author(name, url, iconURL)) =>
        builder.setAuthor(name, url.toStringOrNull, iconURL.toStringOrNull)
      }
      .build()
}

object Embed:
  final case class Author(
    name: String,
    url: Option[URL],
    iconURL: Option[URL],
  )

object EmbedBuilder:
  final class EmbedContext private[boozle] (
    var title: Option[String] = None,
    var description: Option[String] = None,
    var author: Option[Embed.Author] = None,
  ):
    def toEmbed: Embed = Embed(title, description)

  def embed(build: EmbedContext ?=> Unit): Embed =
    given ctx: EmbedContext = EmbedContext()
    build
    ctx.toEmbed

  def title(title: String)(using ctx: EmbedContext): Unit =
    ctx.title = title.some

  def description(description: String)(using ctx: EmbedContext): Unit =
    ctx.description = description.some

  def author(
    name: String,
    url: Option[URL] = None,
    iconURL: Option[URL] = None,
  )(using ctx: EmbedContext): Unit =
    ctx.author = Embed.Author(name, url, iconURL).some

// TODO: builder pattern? lenses?
