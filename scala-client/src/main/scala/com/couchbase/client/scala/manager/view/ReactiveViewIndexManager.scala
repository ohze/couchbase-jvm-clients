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
package com.couchbase.client.scala.manager.view

import java.nio.charset.StandardCharsets.UTF_8

import com.couchbase.client.core.Core
import com.couchbase.client.core.deps.com.fasterxml.jackson.databind.node.ObjectNode
import com.couchbase.client.core.deps.io.netty.buffer.{ByteBuf, Unpooled}
import com.couchbase.client.core.deps.io.netty.handler.codec.http.HttpMethod.GET
import com.couchbase.client.core.deps.io.netty.handler.codec.http._
import com.couchbase.client.core.error.{
  CouchbaseException,
  DesignDocumentNotFoundException,
  ViewServiceException
}
import com.couchbase.client.core.json.Mapper
import com.couchbase.client.core.logging.RedactableArgument.redactMeta
import com.couchbase.client.core.msg.ResponseStatus
import com.couchbase.client.core.msg.view.{GenericViewRequest, GenericViewResponse}
import com.couchbase.client.core.retry.RetryStrategy
import com.couchbase.client.core.util.UrlQueryStringBuilder.urlEncode
import com.couchbase.client.scala.manager.ManagerUtil
import com.couchbase.client.scala.transformers.JacksonTransformers
import com.couchbase.client.scala.util.DurationConversions._
import com.couchbase.client.scala.util.FutureConversions
import com.couchbase.client.scala.view.DesignDocumentNamespace
import reactor.core.scala.publisher.{SFlux, SMono}

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}
import io.circe.{Json, jawn}

class ReactiveViewIndexManager(private[scala] val core: Core, bucket: String) {
  private val DefaultTimeout: Duration =
    core.context().environment().timeoutConfig().managementTimeout()
  private val DefaultRetryStrategy: RetryStrategy = core.context().environment().retryStrategy()

  def getDesignDocument(
      designDocName: String,
      namespace: DesignDocumentNamespace,
      timeout: Duration = DefaultTimeout,
      retryStrategy: RetryStrategy = DefaultRetryStrategy
  ): SMono[DesignDocument] = {
    pathForDesignDocument(designDocName, namespace) match {
      case Success(path) =>
        sendRequest(HttpMethod.GET, path, timeout, retryStrategy)
          .onErrorResume(err => SMono.raiseError(mapNotFoundError(err, designDocName, namespace)))
          .flatMap(response => {
            response.status match {
              case ResponseStatus.SUCCESS =>
                val parsed = jawn
                  .parseByteArray(response.content)
                  .flatMap(ReactiveViewIndexManager.parseDesignDocument(designDocName, _))

                parsed match {
                  case Right(designDoc) => SMono.just(designDoc)
                  case Left(err)        => SMono.raiseError(err)
                }
              case _ =>
                SMono.raiseError(
                  new CouchbaseException(
                    "Failed to drop design document [" +
                      redactMeta(designDocName) + "] from namespace " + namespace
                  )
                )
            }
          })
      case Failure(err) =>
        SMono.raiseError(err)
    }
  }

  def getAllDesignDocuments(
      namespace: DesignDocumentNamespace,
      timeout: Duration = DefaultTimeout,
      retryStrategy: RetryStrategy = DefaultRetryStrategy
  ): SFlux[DesignDocument] = {
    // This particular request goes to port 8091 not 8092, hence use of ManagerUtil.getRequest
    ManagerUtil
      .sendRequest(core, HttpMethod.GET, pathForAllDesignDocuments, timeout, retryStrategy)
      .flatMapMany(response => {
        response.status match {
          case ResponseStatus.SUCCESS =>
            ReactiveViewIndexManager
              .parseAllDesignDocuments(response.content(), namespace) match {
              case Success(docs) => SFlux.fromIterable(docs)
              case Failure(err)  => SFlux.raiseError(err)
            }
          case _ =>
            SFlux.raiseError(
              new CouchbaseException(
                "Failed to get all design documents; response status=" + response.status + "; response body=" + new String(
                  response.content,
                  UTF_8
                )
              )
            )

        }
      })
  }

  def upsertDesignDocument(
      indexData: DesignDocument,
      namespace: DesignDocumentNamespace,
      timeout: Duration = DefaultTimeout,
      retryStrategy: RetryStrategy = DefaultRetryStrategy
  ): SMono[Unit] = {
    pathForDesignDocument(indexData.name, namespace) match {
      case Success(path) =>
        val body = toJson(indexData)
        val request = new GenericViewRequest(
          timeout,
          core.context,
          retryStrategy,
          () => {
            val content: ByteBuf = Unpooled.copiedBuffer(Mapper.encodeAsBytes(body))
            val req =
              new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, path, content)
            req.headers.add("Content-Type", HttpHeaderValues.APPLICATION_JSON)
            req.headers.add("Content-Length", content.readableBytes)
            req
          },
          false,
          bucket
        )

        SMono.defer(() => {
          core.send(request)
          FutureConversions
            .javaCFToScalaMono(request, request.response(), propagateCancellation = true)
            .doOnTerminate(() => request.context().logicallyComplete())
            .map(_ => ())
        })
      case Failure(err) =>
        SMono.raiseError(err)
    }
  }

  def dropDesignDocument(
      designDocName: String,
      namespace: DesignDocumentNamespace,
      timeout: Duration = DefaultTimeout,
      retryStrategy: RetryStrategy = DefaultRetryStrategy
  ): SMono[Unit] = {
    pathForDesignDocument(designDocName, namespace) match {
      case Success(path) =>
        sendRequest(HttpMethod.DELETE, path, timeout, retryStrategy)
          .onErrorResume(err => SMono.raiseError(mapNotFoundError(err, designDocName, namespace)))
          .flatMap(response => {
            response.status match {
              case ResponseStatus.SUCCESS => SMono.just(())
              case _ =>
                SMono.raiseError(
                  new CouchbaseException(
                    "Failed to drop design document [" +
                      redactMeta(designDocName) + "] from namespace " + namespace
                  )
                )
            }
          })
      case Failure(err) =>
        SMono.raiseError(err)
    }
  }

  def mapNotFoundError(
      in: Throwable,
      designDocName: String,
      namespace: DesignDocumentNamespace
  ): Throwable = {
    def default = () => {
      new CouchbaseException(
        s"Failed to drop design document [${redactMeta(designDocName)}] from namespace $namespace"
      )
    }

    in match {
      case x: ViewServiceException =>
        if (x.content.contains("not_found")) {
          DesignDocumentNotFoundException.forName(designDocName, namespace.toString)
        } else default()
      case _ => default()
    }
  }

  def publishDesignDocument(
      designDocName: String,
      timeout: Duration = DefaultTimeout,
      retryStrategy: RetryStrategy = DefaultRetryStrategy
  ): SMono[Unit] = {
    getDesignDocument(designDocName, DesignDocumentNamespace.Development, timeout, retryStrategy)
      .flatMap(
        doc => upsertDesignDocument(doc, DesignDocumentNamespace.Production, timeout, retryStrategy)
      )
  }

  private def pathForDesignDocument(
      name: String,
      namespace: DesignDocumentNamespace
  ): Try[String] = {
    DesignDocumentNamespace
      .requireUnqualified(name)
      .map(unqualifiedName => {
        val adjusted = namespace.adjustName(unqualifiedName)
        "/" + urlEncode(bucket) + "/_design/" + urlEncode(adjusted)
      })
  }

  private def pathForAllDesignDocuments = {
    "/pools/default/buckets/" + urlEncode(bucket) + "/ddocs"
  }

  private def toJson(doc: DesignDocument): ObjectNode = {
    val root  = JacksonTransformers.MAPPER.createObjectNode
    val views = root.putObject("views")
    doc.views.foreach(x => {
      val key      = x._1
      val value    = x._2
      val viewNode = JacksonTransformers.MAPPER.createObjectNode
      viewNode.put("map", value.map)
      value.reduce.foreach((r: String) => viewNode.put("reduce", r))
      views.set(key, viewNode)
      ()
    })
    root
  }

  private def sendRequest(request: GenericViewRequest): SMono[GenericViewResponse] = {
    SMono.defer(() => {
      core.send(request)
      FutureConversions
        .wrap(request, request.response, propagateCancellation = true)
        .doOnTerminate(() => request.context().logicallyComplete())
    })
  }

  private def sendRequest(
      method: HttpMethod,
      path: String,
      timeout: Duration,
      retryStrategy: RetryStrategy
  ): SMono[GenericViewResponse] = {
    sendRequest(
      new GenericViewRequest(
        timeout,
        core.context,
        retryStrategy,
        () => new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path),
        method == GET,
        bucket
      )
    )
  }

  private def sendJsonRequest(
      method: HttpMethod,
      path: String,
      timeout: Duration,
      retryStrategy: RetryStrategy,
      body: Any
  ): SMono[GenericViewResponse] = {
    sendRequest(
      new GenericViewRequest(
        timeout,
        core.context,
        retryStrategy,
        () => {
          val content = Unpooled.copiedBuffer(Mapper.encodeAsBytes(body))
          val req     = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, path, content)
          req.headers.add("Content-Type", HttpHeaderValues.APPLICATION_JSON)
          req.headers.add("Content-Length", content.readableBytes)
          req
        },
        method == GET,
        bucket
      )
    )
  }
}

object ReactiveViewIndexManager {
  private[scala] def parseAllDesignDocuments(
      in: Array[Byte],
      namespace: DesignDocumentNamespace
  ): Try[Seq[DesignDocument]] = {
    def parseRow(row: Json): Option[DesignDocument] = {
      for {
        doc    <- row.hcursor.get[Json]("doc")
        metaId <- doc.hcursor.downField("meta").downField("id").as[String]
        designDocName = metaId.stripPrefix("_design/")
      } yield
        if (namespace.contains(designDocName)) {
          doc.hcursor
            .get[Json]("json")
            .flatMap { designDoc =>
              parseDesignDocument(designDocName, designDoc)
            }
            .toOption
        } else None
    }.toOption.flatten

    val either = for {
      json <- io.circe.jawn.parseByteArray(in)
      rows <- json.hcursor.get[Seq[Json]]("rows")
    } yield rows.flatMap(parseRow)

    either.toTry
  }

  private[scala] def parseDesignDocument(name: String, node: Json) = {
    node.hcursor
      .get[Map[String, View]]("views")
      .map { DesignDocument(name.stripPrefix("dev_"), _) }
  }
}
