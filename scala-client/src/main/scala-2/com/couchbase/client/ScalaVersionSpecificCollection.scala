package com.couchbase.client.scala

import com.couchbase.client.scala.codec.{JsonDeserializer, JsonSerializer}
import com.couchbase.client.scala.datastructures._

import scala.reflect.runtime.universe._

trait ScalaVersionSpecificCollection { this: Collection =>
  /** Returns a [[com.couchbase.client.scala.datastructures.CouchbaseBuffer]] backed by this collection.
    *
    * @param id id of the document underyling the datastructure
    * @param options options for controlling the behaviour of the datastructure
    */
  def buffer[T](id: String, options: Option[CouchbaseCollectionOptions] = None)(
    implicit decode: JsonDeserializer[T],
    encode: JsonSerializer[T],
    tag: WeakTypeTag[T]
  ): CouchbaseBuffer[T] = {
    new CouchbaseBuffer[T](id, this)
  }

  /** Returns a [[com.couchbase.client.scala.datastructures.CouchbaseSet]] backed by this collection.
    *
    * @param id id of the document underyling the datastructure
    * @param options options for controlling the behaviour of the datastructure
    */
  def set[T](id: String, options: Option[CouchbaseCollectionOptions] = None)(
    implicit decode: JsonDeserializer[T],
    encode: JsonSerializer[T],
    tag: WeakTypeTag[T]
  ): CouchbaseSet[T] = {
    new CouchbaseSet[T](id, this)
  }

  /** Returns a [[com.couchbase.client.scala.datastructures.CouchbaseMap]] backed by this collection.
    *
    * @param id id of the document underyling the datastructure
    * @param options options for controlling the behaviour of the datastructure
    */
  def map[T](id: String, options: Option[CouchbaseCollectionOptions] = None)(
    implicit decode: JsonDeserializer[T],
    encode: JsonSerializer[T],
    tag: WeakTypeTag[T]
  ): CouchbaseMap[T] = {
    new CouchbaseMap[T](id, this)
  }

  /** Returns a [[com.couchbase.client.scala.datastructures.CouchbaseQueue]] backed by this collection.
    *
    * @param id id of the document underyling the datastructure
    * @param options options for controlling the behaviour of the datastructure
    */
  def queue[T](id: String, options: Option[CouchbaseCollectionOptions] = None)(
    implicit decode: JsonDeserializer[T],
    encode: JsonSerializer[T],
    tag: WeakTypeTag[T]
  ): CouchbaseQueue[T] = {
    new CouchbaseQueue[T](id, this)
  }
}
