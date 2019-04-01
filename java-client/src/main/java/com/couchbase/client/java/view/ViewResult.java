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

package com.couchbase.client.java.view;

import com.couchbase.client.core.json.Mapper;
import com.couchbase.client.core.msg.view.ViewResponse;
import com.couchbase.client.java.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds a the result of a View request operation if successful.
 *
 * @since 3.0.0
 */
public class ViewResult {

  /**
   * Holds the underlying view response from the core.
   */
  private final ViewResponse response;

  /**
   * Creates a new {@link ViewResult}.
   *
   * @param response the core response.
   */
  ViewResult(final ViewResponse response) {
    this.response = response;
  }

  /**
   * Returns the {@link ViewRow ViewRows} in a blocking, but streaming fashion.
   *
   * @return the {@link Stream} of {@link ViewRow ViewRows}.
   */
  public Stream<ViewRow> rows() {
    return response.rows().map(r -> new ViewRow(r.data())).toStream();
  }

  /**
   * Convenience method to collect all {@link #rows()} into a {@link List}.
   *
   * <p>Be careful when using this method on a large result since it will end up buffering the complete
   * result set in memory. This is very helpful for small queries and exploration, but for larger responses
   * we recommend using either the blocking {@link #rows()} method or the reactive variants for ultimate
   * control.</p>
   *
   * @return a collected list of {@link ViewRow ViewRows}.
   */
  public List<ViewRow> allRows() {
    return rows().collect(Collectors.toList());
  }

  /**
   * Returns the metadata associated with this {@link ViewResult}.
   *
   * @return the metadata associated.
   */
  public ViewMeta meta() {
    return ViewMeta.from(response.header());
  }

  @Override
  public String toString() {
    return "ViewResult{" +
      "rows=" + allRows() +
      ", meta=" + meta() +
      '}';
  }

}