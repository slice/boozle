package ski.ppy
package boozle

import net.dv8tion.jda.api.events.interaction.command.*
import net.dv8tion.jda.api.events.interaction.component.*
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

enum Event:
  case Slash(event: SlashCommandInteractionEvent)
  case Button(event: ButtonInteractionEvent)

import Event.*

extension (b: Button)
  def componentId = b.event.getComponentId

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

extension (e: SlashCommandInteractionEvent)
  def toEvent = Slash(e)

extension (e: ButtonInteractionEvent)
  def toEvent = Button(e)
