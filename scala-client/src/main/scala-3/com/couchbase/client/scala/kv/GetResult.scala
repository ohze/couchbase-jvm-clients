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

package com.couchbase.client.scala.kv

import com.couchbase.client.scala.codec._
import com.couchbase.client.scala.json.JsonObject

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import scala.quoted.{given _, _}

/** The result of a `get` operation, e.g. the contents of a document.
  *
  * @param id         the unique identifier of the document
  * @param cas        the document's CAS value at the time of the lookup
  * @param expiry     if the document was fetched with the `withExpiry` flag set then this will contain the
  *                   document's expiration value.  Otherwise it will be None.
  *
  * @define SupportedTypes this can be of any type for which an implicit
  *                        `com.couchbase.client.scala.codec.JsonDeserializer` can be found: a list
  *                        of types that are supported 'out of the box' is available at
  *                        [[https://docs.couchbase.com/scala-sdk/1.0/howtos/json.html these JSON docs]]
  * @author Graham Pople
  * @since 1.0.0
  */
case class GetResult(
    id: String,
    // It's Right only in the case where projections were requested
    private[scala] val _content: Either[Array[Byte], JsonObject],
    private[scala] val flags: Int,
    cas: Long,
    expiry: Option[Duration],
    transcoder: Transcoder
) {

  /** Return the content, converted into the application's preferred representation.
    *
    * <b>Projections</b>: if the advanced feature projections has been used (e.g. if a `project` array was provided
    * to the `get` call), then the results can only be converted into a
    * `JsonObject` - though it would be possible to change this, please let us know if you'd like to see support for
    * other types.
    *
    * @tparam T $SupportedTypes
    */
  inline def contentAs[T] = ${GetResult.contentAsImpl[T]('this)}
}

object GetResult {
  def contentAsImpl[T : Type](r: Expr[GetResult])(using QuoteContext): Expr[Try[T]] = {
    import qctx.tasty.{Type => _, Try => _, _}
    import scala.quoted.Expr
    import scala.language.implicitConversions

    val deserializer = Expr.summon[JsonDeserializer[T]] match {
      case Some(x) => x
      case None =>
        report.throwError("no implicit argument of type " + typeOf[JsonDeserializer[T]].show +
          " was found for an implicit parameter of method GetResult.contentAs")
    }
    val tt = summon[Type[T]].unseal.tpe
    val isByteArray = typeOf[Array[Byte]] <:< tt
    val isString = typeOf[String] <:< tt
    val isJsonObject = typeOf[JsonObject] <:< tt

    '{$r._content match {
      case Left(bytes) =>
        // Regular case
        $r.transcoder match
          case t: TranscoderWithSerializer =>
            if ${Expr(isByteArray)}
              t.decodeToByteArray(
                bytes,
                $r.flags,
                $deserializer.asInstanceOf[JsonDeserializer[Array[Byte]]]
              ).asInstanceOf[Try[T]]
            else if ${Expr(isString)}
              t.decodeToString(
                bytes,
                $r.flags,
                $deserializer.asInstanceOf[JsonDeserializer[String]]
              ).asInstanceOf[Try[T]]
            else
              t.decode(bytes, $r.flags, $deserializer)

          case t: TranscoderWithoutSerializer =>
            if ${Expr(isByteArray)}
              t.decodeToByteArray(bytes, $r.flags).asInstanceOf[Try[T]]
            else if ${Expr(isString)}
              t.decodeToString(bytes, $r.flags).asInstanceOf[Try[T]]
            else
              t.decode(bytes, $r.flags)

      case Right(obj) =>
        // Projection
        // Check if JsonObject is sub-type of T, which mean T is JsonObject | AnyRef | Any
        if ${Expr(isJsonObject)}
          Success(obj.asInstanceOf[T])
        else
          Failure(
            new IllegalArgumentException(
              "Projection results can currently only be returned with " +
                "contentAs[JsonObject]"
            )
          )
    }}
  }
}
