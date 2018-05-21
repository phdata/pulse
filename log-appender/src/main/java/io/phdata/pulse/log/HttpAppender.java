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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.MDC;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An HTTP log appender implementation for Log4j 1.2.x
 * <p>
 * Configuration example:
 * log4j.appender.http=io.phdata.pulse.log.HttpAppender
 * log4j.appender.http.Address=http://localhost:9999/json
 */
public class HttpAppender extends AppenderSkeleton {

  private final long INITIAL_BACKOFF_TIME_SECONDS = 1;

  private HttpManager httpManager;
  private String address;
  private JsonFactory jsonFactory = new JsonFactory();

  private long lastSuccessfulPost = currentTimeSeconds();
  private boolean isSuccess = true;

  private long backoffTimeSeconds = INITIAL_BACKOFF_TIME_SECONDS;

  public HttpAppender() {
  }

  @Override
  protected void append(LoggingEvent event) {
    try {
      String json = renderJson(event);
      LogLog.debug("JSON content: " + json);

      if (shouldPost()) {
        isSuccess = httpManager.send(json);
        if (isSuccess) {
          lastSuccessfulPost = currentTimeSeconds();
          backoffTimeSeconds = INITIAL_BACKOFF_TIME_SECONDS; // reset backoff time to original value
        } else {
          backoffTimeSeconds = backoffTimeSeconds * 2; // exponential backoff
        }
      }
    } catch (Throwable t) {
      LogLog.error("Unexpected error", t);
    }
  }

  private boolean shouldPost() {
    return (isSuccess || currentTimeSeconds() > lastSuccessfulPost + backoffTimeSeconds);
  }

  /**
   * Translate the LoggingEvent into JSON. Not all info from the event is serialized into JSON.
   *
   * @param event a LoggingEvent
   * @return JSON string representation
   */
  public String renderJson(LoggingEvent event) {
    try {
      StringWriter writer = new StringWriter();
      JsonGenerator jg = jsonFactory.createGenerator(writer);
      jg.writeStartObject();
      jg.writeStringField("category", event.getLoggerName());
      jg.writeStringField("timestamp", Util.epochTimestampToISO8061(event.getTimeStamp()));
      jg.writeStringField("level", event.getLevel().toString());
      jg.writeStringField("message", event.getRenderedMessage());
      jg.writeStringField("threadName", event.getThreadName());
      if (event.getThrowableStrRep() != null) {
        jg.writeStringField("throwable", String.join("\n", event.getThrowableStrRep()));
      }
      jg.writeStringField("ndc", event.getNDC());

      jg.writeObjectFieldStart("properties");
      Set<Entry<String, String>> props = event.getProperties().entrySet();
      for (Entry<String, String> entry : props) {
        jg.writeStringField(entry.getKey(), entry.getValue());
      }

      if (MDC.getContext() != null) {
        Set<Entry<String, String>> mdcProperties = MDC.getContext().entrySet();

        for (Entry<String, String> entry : mdcProperties) {
          jg.writeStringField(entry.getKey(), entry.getValue());

        }
      }

      jg.writeEndObject();


      jg.writeArrayFieldStart("thrown");
      if (event.getThrowableInformation() != null) {
        for (String stackElem : event.getThrowableStrRep()) {
          jg.writeString(stackElem);
        }
      }
      jg.writeEndArray();

      jg.writeEndObject();
      jg.close();
      return writer.toString();
    } catch (IOException ie) {
      LogLog.error("Unexpected error", ie);
      return ie.getMessage();
    }
  }

  @Override
  public void close() {
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

  private Long currentTimeSeconds() {
    return System.currentTimeMillis() / 1000L;
  }

  @Override
  public void activateOptions() {
    httpManager = new HttpManager(URI.create(this.address));
  }

  /**
   * Visible for testing.
   * @param httpManager
   */
  protected void setHttpManager(HttpManager httpManager) {
    this.httpManager = httpManager;
  }
}
