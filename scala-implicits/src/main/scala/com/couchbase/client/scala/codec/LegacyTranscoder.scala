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
package com.couchbase.client.scala.codec

import java.nio.charset.StandardCharsets

import com.couchbase.client.core.msg.kv.CodecFlags

import scala.reflect.runtime.universe._
import scala.util.{Success, Try}

class LegacyTranscoder() extends TranscoderWithSerializer {
  override def encode[T](value: T, serializer: JsonSerializer[T]): Try[EncodedValue] = {
    value match {
      case x: Array[Byte] => Success(EncodedValue(x, CodecFlags.BINARY_COMPAT_FLAGS))
      case x: String =>
        Success(EncodedValue(x.getBytes(StandardCharsets.UTF_8), CodecFlags.STRING_COMPAT_FLAGS))
      case _ =>
        serializer
          .serialize(value)
          .map(bytes => EncodedValue(bytes, CodecFlags.JSON_COMPAT_FLAGS))
    }
  }

  override def decode[T](
      value: Array[Byte],
      flags: Int,
      serializer: JsonDeserializer[T]
  ): Try[T] = {
    serializer.deserialize(value)
  }

  override def decodeToByteArray(
      value: Array[Byte],
      flags: Int,
      deserializer: JsonDeserializer[Array[Byte]]
  ): Try[Array[Byte]] = {
    Success(value)
  }

  override def decodeToString(
      value: Array[Byte],
      flags: Int,
      deserializer: JsonDeserializer[String]
  ): Try[String] = {
    Success(new String(value, StandardCharsets.UTF_8))
  }
}

object LegacyTranscoder {
  val Instance: LegacyTranscoder = new LegacyTranscoder()
}
