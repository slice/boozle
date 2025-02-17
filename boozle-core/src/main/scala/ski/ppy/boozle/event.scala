package ski.ppy
package boozle

import fs2.Stream
import net.dv8tion.jda.api.events.interaction.command.*
import net.dv8tion.jda.api.events.interaction.component.*
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

import scala.reflect.TypeTest

enum Event:
  case Slash(event: SlashCommandInteractionEvent)
  case Button(event: ButtonInteractionEvent)

extension (e: Event)
  def response: IReplyCallback =
    e match {
      case Slash(event)  => event
      case Button(event) => event
    }

  def hook: InteractionHook =
    e match {
      case Slash(event)  => event.getHook
      case Button(event) => event.getHook
    }

import Event.*

extension [F[_]](events: Stream[F, Event])
  def only[E <: Event](using tt: TypeTest[Event, E]): Stream[F, E] =
    events.collect { case (event: E) => event }

extension (b: Button)
  def componentId = b.event.getComponentId

extension (e: SlashCommandInteractionEvent)
  def toEvent = Slash(e)

extension (e: ButtonInteractionEvent)
  def toEvent = Button(e)
