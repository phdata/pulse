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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * An HTTP log appender implementation for Log4j 1.2.x
 * <p>
 * Configuration example:
 * log4j.appender.http=io.phdata.pulse.log.HttpAppender
 * log4j.appender.http.Address=http://localhost:9999/json
 */
public class HttpAppender extends AppenderSkeleton {

  private static final String HOSTNAME = "hostname";
  private static final String SPARK_CONTAINER_ID = "container_id";
  private static final String SPARK_APPLICATION_ID = "application_id";


  private boolean isSparkApplication = false;

  // Visible for testing
  protected boolean verifiedSparkApplication = false;
  private String applicationId = "";
  private String containerId = "";

  private final List<LoggingEvent> buffer = new ArrayList<>();

  private HttpManager httpManager;
  private String address;

  /**
   * JSON serializer
   */
  protected final JsonParser jsonParser = new JsonParser();

  /**
   * Maximum buffer size.
   */
  private int bufferSize = 8192;

  /**
   * Does appender block when buffer is full.
   */
  private boolean blocking = false;

  private String hostname = null;
  private Thread dispatcher;

  public HttpAppender() {
    try {
      this.hostname = java.net.InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      LogLog.error("Could not set hostname: " + e, e);
    }

    this.verifiedSparkApplication = Util.isSparkApplication();
    if (verifiedSparkApplication) {
        this.applicationId = Util.getApplicationId();
        this.containerId = Util.getContainerId();
    }

    try {
      // Shutdown hook will flush log buffers
      Runtime.getRuntime().addShutdownHook(new Thread()
      {
        public void run()
        {
          LogLog.debug("HttpAppender shutdown hook called");
          HttpAppender.this.close();
          LogLog.debug("HttpAppender shutdown hook finished");
        }
      });
    } catch(Exception e) {
      LogLog.debug("Failed to add HttpAppender shutdown hook", e);
    }
  }

  /**
   * Append an event to the buffer.
   * All events will be flushed ff the event is ERROR level or the buffer has reached its max size.
   *
   * @param event The log event.
   */
  @Override
  protected void append(LoggingEvent event) {
    if (event.getProperty(HOSTNAME) == null) {
      event.setProperty(HOSTNAME, hostname);
    }

    if (this.verifiedSparkApplication) {
      event.setProperty(SPARK_APPLICATION_ID, this.applicationId);
      event.setProperty(SPARK_CONTAINER_ID, this.containerId);
    }

    // log immediately if dispatcher thread is dead
    if ((dispatcher == null) || !dispatcher.isAlive() || (bufferSize <= 0)) {
      String json;
      try {
        json = jsonParser.marshallEvent(event);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      httpManager.send(json);
      return;
    }

    // Set the NDC and thread name for the calling thread as these
    // LoggingEvent fields were not set at event creation time.
    event.getNDC();
    event.getThreadName();
    // Get a copy of this thread's MDC.
    event.getMDCCopy();
    event.getRenderedMessage();
    event.getThrowableStrRep();

    synchronized (buffer) {
      while (true) {
        int previousSize = buffer.size();

        if (previousSize < bufferSize) {
          buffer.add(event);

          // if buffer had been empty signal all threads waiting on buffer to check their conditions.
          if (previousSize == 0) {
            buffer.notifyAll();
          }

          break;
        }

        //   Following code is only reachable if buffer is full
        //
        //   if blocking and thread is not already interrupted and not the dispatcher then wait for a buffer notification
        boolean discard = true;
        if (blocking
                && !Thread.interrupted()
                && Thread.currentThread() != dispatcher) {
          try {
            buffer.wait();
            discard = false;
          } catch (InterruptedException e) {
            //  reset interrupt status so calling code can see interrupt on their next wait or sleep.
            Thread.currentThread().interrupt();
          }
        }

        // if blocking is false or thread has been interrupted print warning.
        if (discard) {
          String loggerName = event.getLoggerName();
          LogLog.warn("Discarding LoggingEvent from: " + loggerName);

          break;
        }
      }
    }
  }


  @Override
  public void close() {
    /**
     * Set closed flag and notify all threads to check their conditions.
     * Should result in dispatcher terminating.
     */
    synchronized (buffer) {
      closed = true;
      buffer.notifyAll();
    }

    try {
      dispatcher.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      org.apache.log4j.helpers.LogLog.error(
              "Got an InterruptedException while waiting for the "
                      + "dispatcher to finish.", e);
    }

    try {
      httpManager.close();
    } catch (IOException ie) {
      LogLog.error("Unexpected exception while closing HttpManager.", ie);
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

  /**
   * Force bufferSize to be at least 1.
   *
   * @param size must be greater than zero
   */
  public void setBufferSize(int size) {
    synchronized (buffer) {
      this.bufferSize = (size < 1) ? 1 : size;
      buffer.notifyAll();
    }
  }

  /**
   * Determine if the appender should block when the buffer is full. Default value is true.
   *
   * @param blocking boolean
   */
  public void setBlocking(boolean blocking) {
    synchronized (buffer) {
      this.blocking = blocking;
      buffer.notifyAll();
    }
  }

  @Override
  public void activateOptions() {
    if (this.address == null) {
      throw new NullPointerException("Logger config 'Address' not set");
    } else {
      httpManager = new HttpManager(URI.create(this.address));
    }

    dispatcher = new Thread(new Dispatcher(this, buffer, httpManager), "HTTP appender dispatcher");
    dispatcher.setDaemon(true);
    dispatcher.start();
  }

  /**
   * Visible for testing.
   *
   * @param httpManager handles http connections
   */
  protected void setHttpManager(HttpManager httpManager) {
    this.httpManager = httpManager;
  }

  /**
   * Logging event dispatcher. Operates on Object#wait() and Object#notifiy();
   */
  private static class Dispatcher implements Runnable {

    /**
     * Parent AsyncAppender.
     */
    private final HttpAppender parent;

    /**
     * Event buffer.
     */
    private final List<LoggingEvent> buffer;

    /**
     * Manager of HTTP connections.
     */
    private final HttpManager manager;


    /**
     * Create new instance of dispatcher.
     *
     * @param parent  parent AsyncAppender, may not be null.
     * @param buffer  event buffer, may not be null.
     * @param manager http connection manager, may not be null.
     */
    Dispatcher(final HttpAppender parent, final List<LoggingEvent> buffer, final HttpManager manager) {

      this.parent = parent;
      this.buffer = buffer;
      this.manager = manager;
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
      boolean isActive = true;

      // if interrupted, end thread
      try {
        // loop until the HttpAppender is closed.
        while (isActive) {
          LoggingEvent[] events = null;

          // extract pending events while synchronized on buffer
          synchronized (buffer) {
            int bufferSize = buffer.size();
            isActive = !parent.closed;

            while ((bufferSize == 0) && isActive) {
              buffer.wait();
              bufferSize = buffer.size();
              isActive = !parent.closed;
            }

            if (bufferSize > 0) {
              events = new LoggingEvent[bufferSize];
              buffer.toArray(events);

              buffer.clear();

              // allow blocked appends to continue
              buffer.notifyAll();
            }
          }

          // process events after lock on buffer is released.
          if (events != null) {
            flush(events);
          }
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }

    /**
     * Serialize the log events to JSON and post to the HTTP service.
     */
    void flush(LoggingEvent[] events) {
      String json;
      try {
        json = parent.jsonParser.marshallArray(events);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      manager.send(json);
    }
  }

}
