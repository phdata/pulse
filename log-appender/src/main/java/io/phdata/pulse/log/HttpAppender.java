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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.net.URI;

/**
 * An HTTP log appender implementation for Log4j 1.2.x
 * <p>
 * Configuration example:
 * log4j.appender.http=io.phdata.pulse.log.HttpAppender
 * log4j.appender.http.Address=http://localhost:9999/json
 */
public class HttpAppender extends AppenderSkeleton {

  private final long INITIAL_BACKOFF_TIME_SECONDS = 1;
  private JsonParser jsonParser = new JsonParser();
  private BufferingEventHandler bufferingEventHandler = new BufferingEventHandler();

  private HttpManager httpManager;
  private String address;

  private long lastSuccessfulPostTime = currentTimeSeconds();
  private boolean lastPostSuccess = true;

  private long backoffTimeSeconds = INITIAL_BACKOFF_TIME_SECONDS;

  public HttpAppender() {
    Runtime.getRuntime().addShutdownHook(new Thread()
    {
      public void run()
      {
        close();
      }
    });
  }

  @Override
  protected void append(LoggingEvent event) {
    try {
      bufferingEventHandler.addEvent(event);

      if (shouldFlush()) {
        flush();
      }
    } catch (Throwable t) {
      LogLog.error("Unexpected error", t);
    }
  }

  /**
   * Flush all log events
   * @throws Exception
   */
  private void flush() throws Exception {
    String json = jsonParser.renderArray(bufferingEventHandler.getMessages());
    lastPostSuccess = httpManager.send(json);
    if (lastPostSuccess) {
      lastSuccessfulPostTime = currentTimeSeconds();
      backoffTimeSeconds = INITIAL_BACKOFF_TIME_SECONDS; // reset backoff time to original value
    } else {
      backoffTimeSeconds = backoffTimeSeconds * 2; // exponential backoff
    }
  }

  /**
   * If the log messages should be flushed based on previous errors and how many records the batching event handler contains
   * @return Boolean decision
   */
  private boolean shouldFlush() {
    Long currentTime = currentTimeSeconds();

    return (bufferingEventHandler.shouldFlush() // The batch has grown large enough or enought time has passed
            && lastPostSuccess // the last post was a success
            || currentTime > lastSuccessfulPostTime + backoffTimeSeconds); // enough time has passed after the last failure that we want to try to post again
  }

  @Override
  public void close()  {
    try {
      flush();

    } catch (Exception e) {
      LogLog.error("Unexpected exception while flushing events", e);
    }
    try {
      httpManager.close();
    } catch (IOException ie) {
      LogLog.error("Unexpected exception while closing HttpAppender", ie);
    }
  }

  @Override
  public boolean requiresLayout() {
    return false;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public void setBufferSize(int size) {
    bufferingEventHandler.setBufferSize(size);
  }

  public void setFlushInterval(int interval) {
    bufferingEventHandler.setFlushIntervalMillis(interval);
  }

  private Long currentTimeSeconds() {
    return System.currentTimeMillis() / 1000L;
  }

  @Override
  public void activateOptions() {
    httpManager = new HttpManager(URI.create(this.address));
  }

  /**
   * Visible for testing.
   *
   * @param httpManager
   */
  protected void setHttpManager(HttpManager httpManager) {
    this.httpManager = httpManager;
  }

  /**
   * Visible for testing
   */
  protected void setBatchingEventHandler(BufferingEventHandler eventHandler) {
    this.bufferingEventHandler = eventHandler;
  }
}

