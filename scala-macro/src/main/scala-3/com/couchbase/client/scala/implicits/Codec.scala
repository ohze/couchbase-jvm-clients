/*
 * Copyright (c) 2019 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.scala.implicits

import com.couchbase.client.core.error.DecodingFailureException
import com.couchbase.client.scala.codec.{JsonDeserializer, JsonSerializer}
import io.circe
import io.circe.Printer
import io.circe.syntax._
import scala.util.{Failure, Success, Try}

/** The Scala SDK allows Scala case classes to be directly encoded and decoded to and from the Couchbase Server.
  *
  * But to do this, it needs to be told how to encode and decode the case class to and from JSON.
  *
  * More technically, if you are dealing with a case class `User`, you need an
  * `JsonSerializer[User]` to send it the SDK, and a `JsonDeserializer[User]` to retrieve it.  Or a `Codec[User]`, which conveniently
  * is both.
  *
  * A `Codec[User]` can easily be created like this:
  *
  * {{{
  *   import io.circe.Codec.AsObject
  *
  *   case class Address(line1: String, line2: String) derives AsObject
  *   case class User(name: String, age: Int, address: Address) derives AsObject, Codec
  * }}}
  *
  * Note that a `Codec` is only needed for the top-level case class: e.g. if you are inserting a User, you do
  * not need a `Codec[Address]`.
  *
  * @author Graham Pople
  * @since 1.0.0
  */
object Codec {

  /** Creates a `Codec` for the given type `T`, which is both a `JsonDeserializer[T]` and `JsonSerializer[T]`.  This is everything
    * required to send a case class directly to the Scala SDK, and retrieve results as it. */
  // @deprecated("use Codec.derived instead")
  def codec[T: circe.Decoder: circe.Encoder]: Codec[T] = new CirceBasedCodec[T]

  /** @see [[http://dotty.epfl.ch/docs/reference/contextual/derivation.html Type Class Derivation]] */
  inline def derived[T: circe.Decoder: circe.Encoder]: Codec[T] = new CirceBasedCodec[T]
}

/** A Codec conveniently combines an [[com.couchbase.client.scala.codec.JsonSerializer]] and
  * [[JsonDeserializer]] so that they can be created by [[com.couchbase.client.scala.implicits.Codec.codec]] on the same line.
  */
trait CodecWrapper[-A, B] extends JsonSerializer[A] with JsonDeserializer[B]
trait Codec[A]            extends CodecWrapper[A, A]

/** Codec based on jsoniter-scala's JsonValueCodec */
private[scala] class CirceBasedCodec[T: circe.Decoder: circe.Encoder] extends Codec[T] {
  override def serialize(input: T): Try[Array[Byte]] = Try {
    Printer.noSpaces.printToByteBuffer(input.asJson).array()
  }

  override def deserialize(input: Array[Byte]): Try[T] = {
    io.circe.jawn.decodeByteArray(input) match {
      case Right(result) => Success(result)
      case Left(err)     => Failure(new DecodingFailureException(err))
    }
  }
}
