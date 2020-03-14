package com.couchbase.client.scala.util

import io.circe.{Codec, Decoder, jawn}

import scala.deriving.Mirror

/** Alias to circe.Codec & jawn.decode
  * We don't need this object for scala 3.
  * But with this, we can keep common code (for both scala 2 & 3) unchanged */
private[scala] object CouchbasePickler {
  @inline def read[T: Decoder](input: Array[Byte]): T = jawn.decodeByteArray(input).toTry.get
  @inline def read[T: Decoder](input: String): T = jawn.decode(input).toTry.get
  type ReadWriter[T] = Codec[T]
  inline def macroRW[A](using inline A: Mirror.Of[A]): Codec.AsObject[A] = Codec.AsObject.derived[A]
}
