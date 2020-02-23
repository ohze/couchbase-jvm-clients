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
import com.couchbase.client.scala.util.{CouchbasePickler, given _}
import io.circe
import io.circe.Decoder.Result
import io.circe.{HCursor, Json}

/** Identifies a specific permission possessed by a user.
  *
  * @param name   the role's name
  * @param bucket the bucket the role applies to, if available
  */
@Volatile
case class Role(name: String, bucket: Option[String] = None) {
  def format: String = {
    bucket match {
      case Some(b) => name + "[" + b + "]"
      case _       => name
    }
  }

  override def toString: String = format
}

object Role {
  // Get back "role":"admin" but want to store it as a Role, so custom serialization logic
  implicit val rw: circe.Codec[Role] = new circe.Codec[Role] {
    def apply(c: HCursor): Result[Role] =
      for {
        role   <- c.getOrElse[String]("role")("COULD NOT PARSE")
        bucket <- c.getOrElse[Option[String]]("bucket_name")(None)
      } yield Role(role, bucket)

    def apply(a: Role): Json = {
      val out = circe.JsonObject("role" -> a.name)
      a.bucket.foreach(out.add("bucket", _))
      Json.fromJsonObject(out)
    }
  }
}

/** Associates a role with its display name and description.
  *
  * @param role        the role itself
  * @param displayName the role's display name
  * @param description the role's description
  */
@Volatile
case class RoleAndDescription(role: Role, displayName: String, description: String)

object RoleAndDescription {
  // Get back "role":"admin" but want to store it as a Role, so custom serialization logic
  implicit val rw: circe.Codec[RoleAndDescription] = new circe.Codec[RoleAndDescription] {
    def apply(c: HCursor): Result[RoleAndDescription] =
      for {
        role <- c.getOrElse[String]("role")("COULD NOT PARSE")
        name <- c.getOrElse[String]("name")("COULD NOT PARSE")
        desc <- c.getOrElse[String]("desc")("COULD NOT PARSE")
      } yield RoleAndDescription(Role(role), name, desc)

    def apply(x: RoleAndDescription): Json = Json.obj(
      "role" -> x.role.name,
      "name" -> x.displayName,
      "desc" -> x.description
    )
  }
}

/** Indicates why the user has a specific role.
  *
  * If the type is “user” it means the role is assigned directly to the user. If the type is “group” it means the role
  * is inherited from the group identified by the “name” field.
  *
  * @param typ  the type - "user" or "group"
  * @param name only present if the type is "group"
  */
@Volatile
case class Origin(@upickle.implicits.key("type") typ: String, name: Option[String] = None) {

  override def toString: String = name.map(n => typ + ":" + n).getOrElse(typ)
}

object Origin {
  import circe._

  implicit val rw: Codec[Origin] = {
    val d: Decoder[Origin] = Decoder.forProduct2("type", "name")(Origin.apply)
    val e: Encoder[Origin] = Encoder.forProduct2("type", "name")(x => (x.typ, x.name))
    Codec.from(d, e)
  }
}

/** Associates a role with its origins.
  *
  * @param role    the role
  * @param origins the role's origins
  */
@Volatile
case class RoleAndOrigins(role: Role, origins: Seq[Origin]) {

  /** Returns true if this role is assigned specifically to the user (has origin "user"
    * as opposed to being inherited from a group).
    */
  def innate: Boolean = origins.exists(_.typ == "user")

  override def toString: String =
    role.toString + "<-" + origins.mkString("[", ",", "]") // match Java output
}

object RoleAndOrigins {
  import io.circe.syntax._
  // Get back "role":"admin" but want to store it as a Role, so custom serialization logic
  implicit val rw: circe.Codec[RoleAndOrigins] = new circe.Codec[RoleAndOrigins] {
    def apply(c: HCursor): Result[RoleAndOrigins] =
      for {
        role       <- c.get[String]("role")
        bucketName <- c.get[Option[String]]("bucket_name")
        origins    <- c.getOrElse[Seq[Origin]]("origins")(Seq.empty)
      } yield RoleAndOrigins(Role(role, bucketName), origins)

    def apply(x: RoleAndOrigins): Json = Json.obj(
      "role"    -> x.role.name,
      "origins" -> x.origins.asJson
    )
  }
}
