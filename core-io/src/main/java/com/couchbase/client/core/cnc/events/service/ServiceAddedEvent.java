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

package com.couchbase.client.core.cnc.events.service;

import com.couchbase.client.core.cnc.AbstractEvent;
import com.couchbase.client.core.service.ServiceContext;

import java.time.Duration;

public class ServiceAddedEvent extends AbstractEvent {

  public ServiceAddedEvent(Duration duration, ServiceContext context) {
    super(Severity.DEBUG, Category.SERVICE, duration, context);
  }

  @Override
  public String description() {
    return "Service added to Node";
  }
}
