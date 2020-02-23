package com.couchbase.client.scala.util

import io.circe.Json

import scala.language.implicitConversions

private[scala] object CirceConversions {
//  given Conversion[String, Json] = Json.fromString(_)
  @inline implicit def jsonFromString(s: String): Json = Json.fromString(s)
}
