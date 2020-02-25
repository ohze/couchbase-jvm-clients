package com.couchbase.client.scala.implicits

import io.circe.Codec.AsObject
import java.nio.charset.StandardCharsets.UTF_8

class CodecSpec extends munit.FunSuite {

  case class Address(address: String) derives AsObject

  case class User(name: String, age: Int, addresses: Seq[Address]) derives AsObject, Codec

  val u = User("John Smith", 29, List(Address("123 Fake Street")))

  test("derives Codec") {
    val codec = summon[Codec[User]]

    val bytes = codec.serialize(u).get
    val s = new String(bytes, UTF_8)

    val raw = """{"name":"John Smith","age":29,"addresses":[{"address":"123 Fake Street"}]}"""

    assertEquals(s, raw)
  }
}
