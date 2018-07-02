/*
 * Copyright 2018 phData Inc.
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

package io.phdata.pulse.log;

import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;

public class BatchingEventHandler {
  private Integer batchSize;
  private Integer flushIntervalMillis;
  long lastFlushedMillis;

  private ArrayList<LoggingEvent> messages;

  public BatchingEventHandler(Integer batchSize, Integer flushIntervalMillis) {
    this.batchSize = batchSize;
    this.flushIntervalMillis = flushIntervalMillis;
    lastFlushedMillis = System.currentTimeMillis();
    messages = new ArrayList<>(batchSize);
  }

  synchronized public void addEvent(LoggingEvent event) {
    messages.add(event);
  }

  /**
   * Whether the message queue should be flushed, based on time and size thresholds.
   *
   * @return Boolean decision
   */
  public boolean shouldFlush() {
    long currentTime = System.currentTimeMillis();
    boolean exceededTimeThreshold = (lastFlushedMillis + flushIntervalMillis) < currentTime;
    boolean exceededSizeThreshold = messages.size() > batchSize;

    return (exceededTimeThreshold || exceededSizeThreshold) && messages.size() > 0;
  }

  /**
   * Get all messages from the queue.
   *
   * @return
   */
  synchronized protected LoggingEvent[] getMessages() {
    LoggingEvent[] batchedEvents = new LoggingEvent[messages.size()];
    batchedEvents = messages.toArray(batchedEvents);
    messages.clear();
    lastFlushedMillis = System.currentTimeMillis();
    return batchedEvents;
  }
}
