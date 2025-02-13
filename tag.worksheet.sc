1 + 1

type Tagged[T] = {
  type Tag = T
}

type @@[A, T] = A & Tagged[T]

def command[A, T](f: (@@[A, T]) => Unit)(using v: ValueOf[T]) =
  v.value

command((x: String @@ "hello") => {
  println(x)
})
