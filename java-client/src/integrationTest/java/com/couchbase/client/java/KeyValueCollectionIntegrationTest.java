/*
 * Copyright 2020 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java;

import com.couchbase.client.core.io.CollectionIdentifier;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.util.JavaIntegrationTest;
import com.couchbase.client.test.Capabilities;
import com.couchbase.client.test.IgnoreWhen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@IgnoreWhen(missesCapabilities = Capabilities.COLLECTIONS)
public class KeyValueCollectionIntegrationTest extends JavaIntegrationTest {

  static Cluster cluster;
  static Bucket bucket;

  @BeforeAll
  static void beforeAll() {
    cluster = Cluster.connect(seedNodes(), clusterOptions());
    bucket = cluster.bucket(config().bucketname());
    bucket.waitUntilReady(Duration.ofSeconds(5));
  }

  @AfterAll
  static void afterAll() {
    cluster.disconnect();
  }

  @Test
  void recognizesCollectionAfterCreation() {
    String collId = UUID.randomUUID().toString().substring(0, 10);
    CollectionSpec collectionSpec = CollectionSpec.create(collId, CollectionIdentifier.DEFAULT_SCOPE);
    bucket.collections().createCollection(collectionSpec);

    Collection collection = bucket.collection(collId);

    String id = UUID.randomUUID().toString();
    String content = "bar";
    MutationResult upsertResult = collection.upsert(id, content);
    GetResult getResult = collection.get(id);

    assertEquals(upsertResult.cas(), getResult.cas());
    assertEquals(content, getResult.contentAs(String.class));
  }

}
