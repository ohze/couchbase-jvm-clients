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

package com.couchbase.client.java.kv;

import com.couchbase.client.core.annotation.Stability;
import com.couchbase.client.core.error.CasMismatchException;
import com.couchbase.client.java.codec.Transcoder;

import java.time.Duration;
import java.time.Instant;

import static com.couchbase.client.core.util.Validators.notNull;

public class ReplaceOptions extends CommonDurabilityOptions<ReplaceOptions> {

  private Expiry expiry = Expiry.none();
  private Transcoder transcoder;
  private long cas;

  private ReplaceOptions() { }

  public static ReplaceOptions replaceOptions() {
    return new ReplaceOptions();
  }

  public ReplaceOptions expiry(final Duration expiry) {
    this.expiry = Expiry.relative(expiry);
    return this;
  }

  @Stability.Uncommitted
  public ReplaceOptions expiry(final Instant expiry) {
    this.expiry = Expiry.absolute(expiry);
    return this;
  }

  public ReplaceOptions transcoder(final Transcoder transcoder) {
    notNull(transcoder, "Transcoder");
    this.transcoder = transcoder;
    return this;
  }

  /**
   * Specifies a CAS value that will be taken into account on the server side for optimistic concurrency.
   * <p>
   * The CAS value is an opaque identifier which is associated with a specific state of the document on the server. The
   * CAS value is received on read operations (or after mutations) and can be used during a subsequent mutation to
   * make sure that the document has not been modified in the meantime.
   * <p>
   * If document on the server has been modified in the meantime the SDK will raise a {@link CasMismatchException}. In
   * this case the caller is expected to re-do the whole "fetch-modify-update" cycle again. Please refer to the
   * SDK documentation for more information on CAS mismatches and subsequent retries.
   *
   * @param cas the opaque CAS identifier to use for this operation.
   * @return the {@link ReplaceOptions} for chaining purposes.
   */
  public ReplaceOptions cas(long cas) {
    this.cas = cas;
    return this;
  }

  @Stability.Internal
  public Built build() {
    return new Built();
  }

  public class Built extends BuiltCommonDurabilityOptions {

    Built() { }

    public Expiry expiry() {
      return expiry;
    }

    public Transcoder transcoder() {
      return transcoder;
    }

    public long cas() {
      return cas;
    }

  }
}
