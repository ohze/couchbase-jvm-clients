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
package com.couchbase.client.scala.manager.search

import com.couchbase.client.core.error.CouchbaseException
import com.couchbase.client.scala.codec.JsonDeserializer
import io.circe
import io.circe.Decoder.Result
import io.circe.Json
import io.circe.generic.semiauto

import scala.util.{Failure, Try}

private[scala] case class SearchIndexWrapper(
    indexDef: SearchIndex,
    planPIndexes: Option[Seq[Json]]
) {
  def numPlanPIndexes: Int = planPIndexes match {
    case Some(v) => v.size
    case _       => 0
  }
}

private[scala] object SearchIndexWrapper {
  implicit val rw: circe.Codec[SearchIndexWrapper] = semiauto.deriveCodec
}

private[scala] case class SearchIndexesWrapper(indexDefs: Map[String, SearchIndex])

private[scala] object SearchIndexesWrapper {
  implicit val rw: circe.Codec[SearchIndexesWrapper] = semiauto.deriveCodec
}

case class SearchIndex(
    name: String,
    sourceName: String,
    // The UUID is server-assigned. It should not be present on a created index, but must
    // be present on an updated index.
    uuid: Option[String] = None,
    @upickle.implicits.key("type") typ: Option[String] = None,
    private[scala] val params: Option[Json] = None,
    @upickle.implicits.key("uuid") sourceUUID: Option[String] = None,
    private[scala] val sourceParams: Option[Json] = None,
    sourceType: Option[String] = None,
    private[scala] val planParams: Option[Json] = None,
    private[scala] val numPlanPIndexes: Int = 0
) {
  import SearchIndex._

  private[scala] def toJson: String = rw(this).noSpaces

  def planParamsAs[T](implicit deserializer: JsonDeserializer[T]): Try[T] =
    convert(planParams, deserializer)

  def paramsAs[T](implicit deserializer: JsonDeserializer[T]): Try[T] =
    convert(params, deserializer)

  def sourceParamsAs[T](implicit deserializer: JsonDeserializer[T]): Try[T] =
    convert(sourceParams, deserializer)

  private def convert[T](value: Option[Json], deserializer: JsonDeserializer[T]) = {
    value match {
      case Some(pp) =>
        val bytes = io.circe.Printer.noSpaces.printToByteBuffer(pp).array()
        deserializer.deserialize(bytes)
      case _ => Failure(new CouchbaseException("Index does not contain this field"))
    }
  }
}

object SearchIndex {
  private val DefaultSouceType = "couchbase"
  private val DefaultType      = "fulltext-index"

  def create(name: String, sourceName: String): SearchIndex = {
    SearchIndex(name, sourceName)
  }

  implicit val rw: io.circe.Codec[SearchIndex] = new io.circe.Codec[SearchIndex] {
    import io.circe._

    def apply(c: HCursor): Result[SearchIndex] =
      for {
        name            <- c.get[String]("name")
        sourceName      <- c.get[String]("sourceName")
        uuid            <- c.get[Option[String]]("uuid")
        typ             <- c.get[Option[String]]("type")
        params          <- c.get[Option[Json]]("params")
        sourceParams    <- c.get[Option[Json]]("sourceParams")
        sourceType      <- c.get[Option[String]]("sourceType")
        planParams      <- c.get[Option[Json]]("planParams")
        numPlanPIndexes <- c.getOrElse[Int]("numPlanPIndexes")(0)
      } yield SearchIndex(
        name,
        sourceName,
        uuid,
        typ,
        params,
        uuid,
        sourceParams,
        sourceType,
        planParams,
        numPlanPIndexes
      )

    def apply(a: SearchIndex): Json = {
      import com.couchbase.client.scala.util.CirceConversions._

      val obj = io.circe.JsonObject(
        "name"       -> a.name,
        "sourceName" -> a.sourceName,
        "type"       -> a.typ.getOrElse[String](DefaultType),
        "sourceType" -> a.sourceType.getOrElse[String](DefaultSouceType)
      )
      a.uuid.foreach(obj.add("uuid", _))
      Json.fromJsonObject(obj)
    }
  }
}
