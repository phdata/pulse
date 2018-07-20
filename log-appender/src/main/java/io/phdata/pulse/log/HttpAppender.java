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
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An HTTP log appender implementation for Log4j 1.2.x
 * <p>
 * Configuration example:
 * log4j.appender.http=io.phdata.pulse.log.HttpAppender
 * log4j.appender.http.Address=http://localhost:9999/json
 */
public class HttpAppender extends AppenderSkeleton {

  public static final String HOSTNAME = "hostname";
  public static final long FLUSH_PERIOD_SECONDS = 3;
  public final static long INITIAL_BACKOFF_TIME_SECONDS = 1;

  private final JsonParser jsonParser = new JsonParser();
  private MessageBuffer messageBuffer = new MessageBuffer();

  private HttpManager httpManager;
  private String address;

  private long lastSuccessfulPostTime = currentTimeSeconds();
  private boolean lastPostSuccess = true;

  private long backoffTimeSeconds = INITIAL_BACKOFF_TIME_SECONDS;

  private String hostname = null;
  private final ScheduledFuture scheduledFlushTask;

  public HttpAppender() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        close();
      }
    });

    try {
      hostname = java.net.InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      LogLog.error("Could not set hostname: " + e, e);
    }

    // Flush the buffer every second
    int numCores = 1;
    int initialDelay = 1;
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(numCores);
    scheduledFlushTask = scheduler.scheduleAtFixedRate(new FlusherTask(), initialDelay, FLUSH_PERIOD_SECONDS, TimeUnit.SECONDS);
  }

  /**
   * Append an event to the buffer.
   * All events will be flushed ff the event is ERROR level or the buffer has reached its max size.
   * @param event The log event.
   */
  @Override
  protected void append(LoggingEvent event) {
    try {
      messageBuffer.addEvent(event);
      if (event.getProperty(HOSTNAME) == null) {
        event.setProperty(HOSTNAME, hostname);
      }
      if (event.getLevel().isGreaterOrEqual(Level.ERROR) || messageBuffer.isFull()) {
        flush();
      }
    } catch (Throwable t) {
      LogLog.error("Unexpected error: " + t, t);
    }
  }

  /**
   * Flush all log events. If the last flush was a failure use exponential backoff.
   *
   * @throws Exception
   */
  void flush() throws Exception {
    if (lastPostSuccess || currentTimeSeconds() >= lastSuccessfulPostTime + backoffTimeSeconds) {
      lastPostSuccess = forceFlush();
      if (lastPostSuccess) {
        lastSuccessfulPostTime = currentTimeSeconds();
        backoffTimeSeconds = INITIAL_BACKOFF_TIME_SECONDS; // reset backoff time to original value
      } else {
        backoffTimeSeconds = backoffTimeSeconds * 2; // exponential backoff
      }
    }
  }

  /**
   * Flush messages
   * @throws Exception
   */
  boolean forceFlush() throws Exception {
    String json = jsonParser.marshallArray(messageBuffer.getMessages());
    return httpManager.send(json);
  }

  @Override
  public void close() {
    try {
      forceFlush();
    } catch (Exception e) {
      LogLog.error("Unexpected exception while flushing events: " + e, e);
    } catch (Error e) {
      LogLog.error("Unexpected error while flushing events: " + e, e);
    }
    try {
      scheduledFlushTask.cancel(false);
    } catch (Exception ie) {
      LogLog.error("Unexpected exception while cancelling scheduled tryFlush task: " + ie, ie);
    }
    try {
      httpManager.close();
    } catch (IOException ie) {
      LogLog.error("Unexpected exception while closing HttpAppender: " + ie, ie);
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
    messageBuffer.setBufferSize(size);
  }

  private Long currentTimeSeconds() {
    return System.currentTimeMillis() / 1000L;
  }

  @Override
  public void activateOptions() {
    if (this.address == null) {
      throw new NullPointerException("Logger config 'Address' not set");
    } else {
      httpManager = new HttpManager(URI.create(this.address));
    }
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
  protected void setMessageBuffer(MessageBuffer messageBuffer) {
    this.messageBuffer = messageBuffer;
  }

  class FlusherTask implements Runnable {
    @Override
    public void run() {
      try {
        flush();
      } catch (Throwable t) {
        LogLog.error("Unexpected error: " + t, t);
      }
    }
  }
}

