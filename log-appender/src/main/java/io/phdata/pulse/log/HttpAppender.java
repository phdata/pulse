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

import io.phdata.pulse.HttpStream;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

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

    private HttpManager httpManager;
    private String address;
    private HttpStream stream;

    /**
     * JSON serializer
     */
    protected final JsonParser jsonParser = new JsonParser();

    /**
     * Maximum buffer size.
     */
    private int bufferSize = 8192;
    private FiniteDuration flushDuration = Duration.create(3, "seconds");

    /**
     * Flush size
     */
    private int flushSize = 100;

    private String hostname = null;

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
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    LogLog.debug("HttpAppender shutdown hook called");
                    HttpAppender.this.close();
                    LogLog.debug("HttpAppender shutdown hook finished");
                }
            });
        } catch (Exception e) {
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

        // Set the NDC and thread name for the calling thread as these
        // LoggingEvent fields were not set at event creation time.
        event.getNDC();
        event.getThreadName();
        // Get a copy of this thread's MDC.
        event.getMDCCopy();
        event.getRenderedMessage();
        event.getThrowableStrRep();

        stream.append(event);
    }


    @Override
    public void close() {
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

        if (this.bufferSize < 1) {
            throw new IllegalArgumentException("Buffer size must be greater than zero");
        }
        this.bufferSize = size;
    }

    /**
     * Force bufferSize to be at least 1.
     *
     * @param size must be greater than zero
     */
    public void setbuffersize(int size) {
        this.setBufferSize(size);
    }

    @Override
    public void activateOptions() {
        if (this.getAddress() == null) {
            throw new NullPointerException("Logger config 'Address' not set");
        } else {
            httpManager = new HttpManager(URI.create(this.address));
            this.stream = new HttpStream(this.flushDuration, this.flushSize, this.bufferSize, this.httpManager);
        }
    }

    /**
     * Visible for testing.
     *
     * @param httpStream handles http connections
     */

    protected void setHttpStream(HttpStream httpStream) {
        this.stream = httpStream;
    }

    protected HttpManager getHttpManager() {
        return httpManager;
    }
}
