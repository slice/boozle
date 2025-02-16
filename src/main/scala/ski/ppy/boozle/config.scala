package ski.ppy
package boozle

import fabric.rw.*

case class Config(
  token: String,
  register: Boolean
)

object Config {
  given RW[Config] = RW.gen[Config]
}
