package com.couchbase.client.scala.util

import io.circe.generic.codec.DerivedAsObjectCodec
import io.circe.{Codec, Decoder, jawn}
import shapeless.Lazy

/** Alias to circe.Codec & jawn.decode
  * We don't need this object for scala 3.
  * But with this, we can keep common code (for both scala 2 & 3) unchanged */
private[scala] object CouchbasePickler {
  @inline def read[T: Decoder](input: Array[Byte]): T = jawn.decodeByteArray(input).toTry.get
  @inline def read[T: Decoder](input: String): T = jawn.decode(input).toTry.get
  type ReadWriter[T] = Codec[T]
  @inline final def macroRW[A](implicit codec: Lazy[DerivedAsObjectCodec[A]]): Codec.AsObject[A] = codec.value
}
