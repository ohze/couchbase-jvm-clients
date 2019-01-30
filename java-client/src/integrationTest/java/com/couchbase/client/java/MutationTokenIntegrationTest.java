/*
 * Copyright (c) 2018 Couchbase, Inc.
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

package com.couchbase.client.java;

import com.couchbase.client.core.msg.kv.MutationToken;
import com.couchbase.client.java.codec.BinaryContent;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.CounterResult;
import com.couchbase.client.java.kv.MutateInResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.util.JavaIntegrationTest;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static com.couchbase.client.java.kv.DecrementOptions.decrementOptions;
import static com.couchbase.client.java.kv.IncrementOptions.incrementOptions;
import static com.couchbase.client.java.kv.MutateInOps.mutateInOps;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MutationTokenIntegrationTest extends JavaIntegrationTest {

  private Cluster cluster;
  private ClusterEnvironment environment;
  private Collection collection;

  @BeforeEach
  void beforeEach() {
    environment = environment().mutationTokensEnabled(true).build();
    cluster = Cluster.connect(environment);
    Bucket bucket = cluster.bucket(config().bucketname());
    collection = bucket.defaultCollection();
  }

  @AfterEach
  void afterEach() {
    environment.shutdown(Duration.ofSeconds(1));
    cluster.shutdown();
  }

  @Test
  void tokenOnAppend() {
    String id = UUID.randomUUID().toString();

    byte[] helloBytes = "Hello, ".getBytes(CharsetUtil.UTF_8);
    byte[] worldBytes = "World!".getBytes(CharsetUtil.UTF_8);

    MutationResult upsert = collection.upsert(id, BinaryContent.wrap(helloBytes));
    assertMutationToken(upsert.mutationToken());

    MutationResult append = collection.binary().append(id, worldBytes);
    assertMutationToken(append.mutationToken());
  }

  @Test
  void tokenOnPrepend() {
    String id = UUID.randomUUID().toString();

    byte[] helloBytes = "Hello, ".getBytes(CharsetUtil.UTF_8);
    byte[] worldBytes = "World!".getBytes(CharsetUtil.UTF_8);

    MutationResult upsert = collection.upsert(id, BinaryContent.wrap(helloBytes));
    assertMutationToken(upsert.mutationToken());

    MutationResult prepend = collection.binary().prepend(id, worldBytes);
    assertMutationToken(prepend.mutationToken());
  }

  @Test
  void tokenOnSubdocMutate() {
    String id = UUID.randomUUID().toString();
    MutationResult result = collection.upsert(id, JsonObject.empty());
    assertMutationToken(result.mutationToken());

    MutateInResult mutateResult = collection.mutateIn(id, mutateInOps().insert("foo", true));
    assertMutationToken(mutateResult.mutationToken());
  }

  @Test
  void tokenOnUpsert() {
    String id = UUID.randomUUID().toString();
    MutationResult result = collection.upsert(id, JsonObject.empty());
    assertMutationToken(result.mutationToken());
  }

  @Test
  void tokenOnReplace() {
    String id = UUID.randomUUID().toString();
    MutationResult result = collection.upsert(id, JsonObject.empty());
    assertMutationToken(result.mutationToken());

    MutationResult replace = collection.replace(id, JsonObject.create().put("foo", true));
    assertMutationToken(replace.mutationToken());
  }

  @Test
  void tokenOnRemove() {
    String id = UUID.randomUUID().toString();
    MutationResult result = collection.upsert(id, JsonObject.empty());
    assertMutationToken(result.mutationToken());

    MutationResult remove = collection.remove(id);
    assertMutationToken(remove.mutationToken());
  }

  @Test
  void tokenOnInsert() {
    String id = UUID.randomUUID().toString();
    MutationResult result = collection.insert(id, JsonObject.empty());
    assertMutationToken(result.mutationToken());
  }

  @Test
  void tokenOnIncrement() {
    String id = UUID.randomUUID().toString();
    CounterResult result = collection.binary().increment(
      id,
      incrementOptions().initial(Optional.of(1L))
    );
    assertMutationToken(result.mutationToken());
  }

  @Test
  void tokenOnDecrement() {
    String id = UUID.randomUUID().toString();
    CounterResult result = collection.binary().decrement(
      id,
      decrementOptions().initial(Optional.of(1L))
    );
    assertMutationToken(result.mutationToken());
  }

  void assertMutationToken(final Optional<MutationToken> token) {
    assertTrue(token.isPresent());
    token.ifPresent(t -> {
      assertEquals(config().bucketname(), t.bucket());
      assertTrue(t.vbucketID() >= 0);
      assertTrue(t.sequenceNumber() > 0);
      assertTrue(t.vbucketUUID() != 0);
    });
  }

}