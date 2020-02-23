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
package com.couchbase.client.scala.manager.analytics
import com.couchbase.client.scala.implicits.Codec
import com.github.plokhotnyuk.jsoniter_scala.macros.named

case class AnalyticsDataset(
    @named("DatasetName") name: String,
    @named("DataverseName") dataverseName: String,
    @named("LinkName") linkName: String,
    @named("BucketName") bucketName: String
)

object AnalyticsDataset {
  import io.circe.{Codec => CirceCodec}

  private implicit val circeCodec: CirceCodec[AnalyticsDataset] = {
    import io.circe._
    val decoder: Decoder[AnalyticsDataset] =
      Decoder.forProduct4("DatasetName", "DataverseName", "LinkName", "BucketName")(
        AnalyticsDataset.apply
      )
    val encoder: Encoder[AnalyticsDataset] =
      Encoder.forProduct4("DatasetName", "DataverseName", "LinkName", "BucketName")(
        d => (d.name, d.dataverseName, d.linkName, d.bucketName)
      )
    CirceCodec.from(decoder, encoder)
  }
  implicit val codec: Codec[AnalyticsDataset] = Codec.codec[AnalyticsDataset]
}
