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

package com.couchbase.client.core.util;

import com.couchbase.client.core.env.CoreEnvironment;
import com.couchbase.client.core.env.SeedNode;
import com.couchbase.client.test.ClusterAwareIntegrationTest;
import com.couchbase.client.test.Services;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extends the {@link ClusterAwareIntegrationTest} with core-io specific code.
 *
 * @since 2.0.0
 */
public class CoreIntegrationTest extends ClusterAwareIntegrationTest {

  /**
   * Calculates the default collection id.
   */
  public static byte[] DEFAULT_COLLECTION_ID = UnsignedLEB128.encode(BigInteger.valueOf(0));

  /**
   * Creates an environment builder that already has all the needed properties to bootstrap
   * set.
   */
  protected CoreEnvironment.Builder environment() {
    Set<SeedNode> seeds = config().nodes().stream().map(cfg -> SeedNode.create(
      cfg.hostname(),
      Optional.of(cfg.ports().get(Services.KV)),
      Optional.of(cfg.ports().get(Services.MANAGER))
    )).collect(Collectors.toSet());

    return CoreEnvironment
      .builder(config().adminUsername(), config().adminPassword())
      .seedNodes(seeds);
  }

}
