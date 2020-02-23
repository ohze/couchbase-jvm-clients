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

import com.couchbase.client.core.error.DecodingFailureException
import com.couchbase.client.core.msg.kv.CodecFlags

import scala.util.{Failure, Success, Try}

class RawBinaryTranscoder extends TranscoderWithoutSerializer {
  override def encode[T](value: T): Try[EncodedValue] = {
    value match {
      case x: Array[Byte] => Success(EncodedValue(x, CodecFlags.BINARY_COMPAT_FLAGS))
      case _ =>
        Failure(
          new IllegalArgumentException("Only Array[Byte] is supported for the RawBinaryTranscoder!")
        )
    }
  }

  override def decode[A](value: Array[Byte], flags: Int): Try[A] = {
    Failure(new DecodingFailureException("RawBinaryTranscoder can only decode into Array[Byte]!"))
  }

  override def decodeToByteArray(value: Array[Byte], flags: Int): Try[Array[Byte]] = {
    Success(value)
  }

  override def decodeToString(value: Array[Byte], flags: Int): Try[String] = {
    Failure(new DecodingFailureException("RawBinaryTranscoder can only decode into Array[Byte]!"))
  }
}

object RawBinaryTranscoder {
  val Instance: RawBinaryTranscoder = new RawBinaryTranscoder()
}
