package com.couchbase.client.scala.implicits

import scala.language.experimental.macros

trait ScalaVersionSpecificCodec {

  /** Creates a `Codec` for the given type `T`, which is both a `JsonDeserializer[T]` and `JsonSerializer[T]`.  This is everything
    * required to send a case class directly to the Scala SDK, and retrieve results as it.
    */
  def codec[T]: Codec[T] = macro CodecImplicits.makeCodec[T]
}

private[scala] object CodecImplicits {
  // SCBC-158: Note on withSetMaxInsertNumber(100000) below.  The default number of items allowed in Sets and Maps is a
  // conservative 1,024.  Adjusting this to an arbitrary 100k.

  // Implementation detail: the excellent JSON library com.github.plokhotnyuk.jsoniter_scala
  // is currently used to encode and decode case classes.  This is purely an implementation detail and should not be
  // relied upon.
  def makeCodec[T](
      c: scala.reflect.macros.blackbox.Context
  )(implicit e: c.WeakTypeTag[T]): c.universe.Tree = {
    import c.universe._
    q"""
    new Codec[$e] {
      import com.github.plokhotnyuk.jsoniter_scala.core.{JsonValueCodec, readFromArray, writeToArray}
      import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

      import scala.util.Try

      val jsonIterCodec: JsonValueCodec[$e] =
        JsonCodecMaker.make[$e](
          CodecMakerConfig
            .withSetMaxInsertNumber(100000)
            .withMapMaxInsertNumber(100000)
        )

      override def serialize(input: $e): Try[Array[Byte]] = {
        Try(writeToArray(input)(jsonIterCodec))
      }

      override def deserialize(input: Array[Byte]): Try[$e] = {
        Try(readFromArray(input)(jsonIterCodec))
      }
    }
    """
  }
}
