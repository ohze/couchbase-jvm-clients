package com.couchbase.client.scala.implicits

import io.circe
import com.couchbase.client.core.error.DecodingFailureException
import io.circe.Printer
import io.circe.syntax._
import scala.util.{Failure, Success, Try}

private[scala] trait ScalaVersionSpecificCodec {

  /** Creates a `Codec` for the given type `T`, which is both a `JsonDeserializer[T]` and `JsonSerializer[T]`.  This is everything
    * required to send a case class directly to the Scala SDK, and retrieve results as it.
    */
  def codec[T: circe.Codec]: Codec[T] = new CirceBasedCodec[T]
}

/** Codec based on jsoniter-scala's JsonValueCodec */
private[scala] class CirceBasedCodec[T: circe.Codec] extends Codec[T] {
  override def serialize(input: T): Try[Array[Byte]] = Try {
    Printer.noSpaces.printToByteBuffer(input.asJson).array()
  }

  override def deserialize(input: Array[Byte]): Try[T] = {
    io.circe.jawn.decodeByteArray(input) match {
      case Right(result) => Success(result)
      case Left(err)     => Failure(new DecodingFailureException(err))
    }
  }
}
