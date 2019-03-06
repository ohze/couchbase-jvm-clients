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

package com.couchbase.client.core.msg.kv;

import com.couchbase.client.core.CoreContext;
import com.couchbase.client.core.error.DurabilityLevelNotAvailableException;
import com.couchbase.client.core.io.netty.kv.ChannelContext;
import com.couchbase.client.core.io.netty.kv.MemcacheProtocol;
import com.couchbase.client.core.retry.RetryStrategy;
import com.couchbase.client.core.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.core.deps.io.netty.buffer.ByteBufAllocator;
import com.couchbase.client.core.deps.io.netty.buffer.ByteBufUtil;
import com.couchbase.client.core.deps.io.netty.buffer.Unpooled;

import java.time.Duration;
import java.util.Optional;

import static com.couchbase.client.core.io.netty.kv.MemcacheProtocol.*;

public class DecrementRequest extends BaseKeyValueRequest<DecrementResponse> {

  private final long delta;
  private final Optional<Long> initial;
  private final int expiry;
  private final Optional<DurabilityLevel> syncReplicationType;
  private final long cas;


  public DecrementRequest(Duration timeout, CoreContext ctx, String bucket,
                          RetryStrategy retryStrategy, String key, long cas, byte[] collection,
                          long delta, Optional<Long> initial, int expiry,
                          final Optional<DurabilityLevel> syncReplicationType) {
    super(timeout, ctx, bucket, retryStrategy, key, collection);
    if (initial.isPresent() && initial.get() < 0) {
      throw new IllegalArgumentException("The initial needs to be >= 0");
    }
    this.delta = delta;
    this.initial = initial;
    this.expiry = expiry;
    this.syncReplicationType = syncReplicationType;
    this.cas = cas;
  }

  @Override
  public ByteBuf encode(ByteBufAllocator alloc, int opaque, ChannelContext ctx) {
    ByteBuf key = Unpooled.wrappedBuffer(ctx.collectionsEnabled() ? keyWithCollection() : key());
    ByteBuf extras = alloc.buffer();
    extras.writeLong(delta);
    if (initial.isPresent()) {
      extras.writeLong(initial.get());
      extras.writeInt(expiry);
    } else {
      extras.writeLong(0); // no initial present, will lead to doc not found
      extras.writeInt(IncrementRequest.COUNTER_NOT_EXISTS_EXPIRY);
    }

    ByteBuf request;
    if (syncReplicationType.isPresent()) {
      if (ctx.syncReplicationEnabled()) {
        Duration timeoutAdjusted = RequestUtil.handleDurabilityTimeout(context(), timeout());
        ByteBuf flexibleExtras = flexibleSyncReplication(alloc, syncReplicationType.get(), timeoutAdjusted);
        request = MemcacheProtocol.flexibleRequest(alloc, MemcacheProtocol.Opcode.DECREMENT, noDatatype(),
                partition(), opaque, cas, flexibleExtras, extras, key, noBody());
        flexibleExtras.release();
      }
      else {
        throw new DurabilityLevelNotAvailableException(syncReplicationType.get());
      }
    } else {
      request = MemcacheProtocol.request(alloc, MemcacheProtocol.Opcode.DECREMENT, noDatatype(),
        partition(), opaque, cas, extras, key, noBody());
    }

    extras.release();
    key.release();
    return request;
  }

  @Override
  public DecrementResponse decode(ByteBuf response, ChannelContext ctx) {
    return new DecrementResponse(
      decodeStatus(response),
      body(response).map(ByteBuf::readLong).orElse(0L),
      cas(response),
      extractToken(ctx.mutationTokensEnabled(), partition(), response, ctx.bucket())
    );
  }

}
