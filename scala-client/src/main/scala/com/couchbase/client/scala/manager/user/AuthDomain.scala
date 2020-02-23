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

package com.couchbase.client.scala.manager.user

import com.couchbase.client.core.annotation.Stability.Volatile
import io.circe

sealed trait AuthDomain {
  def alias: String
}

object AuthDomain {

  @Volatile
  case object Local extends AuthDomain {
    def alias: String = "local"
  }

  @Volatile
  case object External extends AuthDomain {
    def alias: String = "external"
  }

  implicit val rw: circe.Codec[AuthDomain] = circe.Codec.from(
    circe.Decoder.decodeString.map {
      case "local"    => Local
      case "external" => External
    },
    circe.Encoder.encodeString.contramap[AuthDomain](_.alias)
  )
}
