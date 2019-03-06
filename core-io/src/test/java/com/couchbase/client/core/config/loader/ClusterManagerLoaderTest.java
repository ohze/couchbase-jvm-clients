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

package com.couchbase.client.core.config.loader;

import com.couchbase.client.core.Core;
import com.couchbase.client.core.CoreContext;
import com.couchbase.client.core.env.CoreEnvironment;
import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.core.error.ConfigException;
import com.couchbase.client.core.io.NetworkAddress;
import com.couchbase.client.core.msg.CancellationReason;
import com.couchbase.client.core.msg.ResponseStatus;
import com.couchbase.client.core.msg.manager.TerseBucketConfigRequest;
import com.couchbase.client.core.msg.manager.TerseBucketConfigResponse;
import com.couchbase.client.core.retry.BestEffortRetryStrategy;
import com.couchbase.client.core.deps.io.netty.util.CharsetUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the functionality of the {@link ClusterManagerLoader}.
 */
class ClusterManagerLoaderTest {

  private static final NetworkAddress SEED = mock(NetworkAddress.class);
  private static final String BUCKET = "bucket";

  private ClusterManagerLoader loader;
  private Core core;

  @BeforeEach
  void setup() {
    CoreEnvironment env = mock(CoreEnvironment.class);
    when(env.timeoutConfig()).thenReturn(TimeoutConfig.create());
    when(env.retryStrategy()).thenReturn(BestEffortRetryStrategy.INSTANCE);

    core = mock(Core.class);
    CoreContext ctx = new CoreContext(core, 1, env);
    when(core.context()).thenReturn(ctx);
    loader = new ClusterManagerLoader(core);
  }

  @Test
  void loadsConfigSuccessfully() {
    byte[] expectedConfig = "config".getBytes(CharsetUtil.UTF_8);

    TerseBucketConfigResponse response = mock(TerseBucketConfigResponse.class);
    when(response.status()).thenReturn(ResponseStatus.SUCCESS);
    when(response.config()).thenReturn(expectedConfig);

    doAnswer(i -> {
      ((TerseBucketConfigRequest) i.getArgument(0)).succeed(response);
      return null;
    }).when(core).send(any(TerseBucketConfigRequest.class));

    byte[] config = loader.discoverConfig(SEED, BUCKET).block();
    assertArrayEquals(expectedConfig, config);
  }

  @Test
  void errorsIfNonSuccessful() {
    TerseBucketConfigResponse response = mock(TerseBucketConfigResponse.class);
    when(response.status()).thenReturn(ResponseStatus.UNKNOWN);

    doAnswer(i -> {
      ((TerseBucketConfigRequest) i.getArgument(0)).succeed(response);
      return null;
    }).when(core).send(any(TerseBucketConfigRequest.class));

    assertThrows(ConfigException.class, () -> loader.discoverConfig(SEED, BUCKET).block());
  }

  @Test
  void errorsIfFailedRequest() {
    doAnswer(i -> {
      ((TerseBucketConfigRequest) i.getArgument(0))
        .fail(new UnsupportedOperationException());
      return null;
    }).when(core).send(any(TerseBucketConfigRequest.class));

    assertThrows(
      UnsupportedOperationException.class,
      () -> loader.discoverConfig(SEED, BUCKET).block()
    );
  }

  /**
   * Since the client may run many loaders in parallel, once a good config is found the other
   * attempts will be stopped.
   *
   * <p>This test makes sure that if an operation is ongoing but the downstream listener
   * unsubscribes, it gets cancelled so we are not performing any loader ops that are not needed
   * anymore.</p>
   */
  @Test
  void cancelRequestOnceUnsubscribed() {
    final AtomicReference<TerseBucketConfigRequest> request = new AtomicReference<>();
    doAnswer(i -> {
      request.set(i.getArgument(0));
      return null;
    }).when(core).send(any(TerseBucketConfigRequest.class));

    Disposable disposable = loader.discoverConfig(SEED, BUCKET).subscribe();
    disposable.dispose();

    assertTrue(request.get().completed());
    assertTrue(request.get().cancelled());
    assertEquals(CancellationReason.STOPPED_LISTENING, request.get().cancellationReason());
  }
}