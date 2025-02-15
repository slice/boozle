package ski.ppy.mootiepop

import scala.quoted.*

def showI(x: Expr[Any])(using Quotes): Expr[Any] = {
  println(x.show)
  x
}

inline def show(inline x: Any): Any = ${ showI('x) }
