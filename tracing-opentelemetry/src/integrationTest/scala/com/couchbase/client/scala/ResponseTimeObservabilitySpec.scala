/*
 * Copyright (c) 2020 Couchbase, Inc.
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
package com.couchbase.client.scala

import java.util.UUID

import com.couchbase.client.scala.env.ClusterEnvironment
import com.couchbase.client.scala.util.ScalaIntegrationTest
import com.couchbase.client.test.{Capabilities, IgnoreWhen, Util}
import com.couchbase.client.tracing.opentelemetry.OpenTelemetryRequestTracer
import io.opentelemetry.exporters.inmemory.InMemorySpanExporter
import io.opentelemetry.sdk.trace.TracerSdkFactory
import io.opentelemetry.sdk.trace.`export`.SimpleSpansProcessor
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api._

@TestInstance(Lifecycle.PER_CLASS)
class ResponseTimeObservabilitySpec extends ScalaIntegrationTest {

  private var cluster: Cluster = _
  private var coll: Collection = _
  private val exporter         = InMemorySpanExporter.create()

  override protected def environment: ClusterEnvironment.Builder = {
    val tracer = TracerSdkFactory.create
    tracer.addSpanProcessor(SimpleSpansProcessor.newBuilder(exporter).build)

    ClusterEnvironment.builder
      .requestTracer(OpenTelemetryRequestTracer.wrap(tracer.get("integrationTest")))
  }

  @BeforeAll
  def beforeAll(): Unit = {
    cluster = connectToCluster()

    val bucket = cluster.bucket(config.bucketname)
    coll = bucket.defaultCollection
  }

  @AfterAll
  def afterAll(): Unit = {
    cluster.disconnect()
  }

  @BeforeEach
  def beforeEach() = {
    exporter.reset()
  }

  private def waitForEvents(numEvents: Int): Unit = {
    Util.waitUntilCondition(() => {
      val size = exporter.getFinishedSpanItems().size()
      size == numEvents
    })
    exporter.reset()
  }

  @Test
  def basicKV(): Unit = {
    val docId = UUID.randomUUID().toString

    val content = ujson.Obj("hello" -> "world")
    coll.insert(docId, content).get
    waitForEvents(3)

    coll.get(docId).get
    waitForEvents(2)

    coll.remove(docId)
    waitForEvents(2)
  }

  @Test
  def reactiveKV(): Unit = {
    val docId = UUID.randomUUID().toString

    val content = ujson.Obj("hello" -> "world")
    coll.reactive.insert(docId, content).block()
    waitForEvents(3)

    coll.reactive.get(docId).block()
    waitForEvents(2)

    coll.reactive.remove(docId).block()
    waitForEvents(2)
  }
  @Test
  @IgnoreWhen(missesCapabilities = Array(Capabilities.QUERY))
  def query(): Unit = {
    cluster.query("select 'hello' as greeting").get
    waitForEvents(1)

    cluster.reactive.query("select 'hello' as greeting").block()
    waitForEvents(1)
  }

  @Test
  @IgnoreWhen(missesCapabilities = Array(Capabilities.ANALYTICS))
  def analytics(): Unit = {
    cluster.analyticsQuery("select 'hello' as greeting").get
    waitForEvents(1)

    cluster.reactive.analyticsQuery("select 'hello' as greeting").block()
    waitForEvents(1)
  }
}
