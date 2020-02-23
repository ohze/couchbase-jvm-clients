package com.couchbase.client.scala.util

import io.circe.{Codec, Decoder, Json, jawn}

import scala.deriving.Mirror

object CouchbasePickler {
  def read[T: Decoder](input: Array[Byte]): T = jawn.decodeByteArray(input).toTry.get
  def read[T: Decoder](input: String): T = jawn.decode(input).toTry.get
  type ReadWriter[T] = Codec[T]
  inline final def macroRW[A](given inline A: Mirror.Of[A]): Codec.AsObject[A] = Codec.AsObject.derived[A]
}

given Conversion[String, Json] = Json.fromString(_)
//  import scala.language.implicitConversions
//  private implicit def jsonFromString(s: String): Json = Json.fromString(s)
