package io.phdata.pulse.log;/* Copyright 2018 phData Inc. */

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.log4j.MDC;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

public class JsonParser {
  private JsonFactory jsonFactory = new JsonFactory();

  public String marshallArray(LoggingEvent[] events) throws Exception {
    StringWriter writer = new StringWriter();
    JsonGenerator jg = jsonFactory.createGenerator(writer);

    jg.writeStartArray();

    for (LoggingEvent event : events) {
      marshallEventInternal(event, jg);
    }

    jg.writeEndArray();

    jg.close();

    return writer.toString();
  }

  /**
   * Translate the LoggingEvent into JSON. Not all info from the event is serialized into JSON.
   *
   * @param event a LoggingEvent
   * @return JSON string representation
   */
  public String marshallEvent(LoggingEvent event) throws IOException {
    StringWriter writer = new StringWriter();
    JsonGenerator jg = jsonFactory.createGenerator(writer);

    marshallEventInternal(event, jg);

    jg.close();
    return writer.toString();
  }


  private void marshallEventInternal(LoggingEvent event, JsonGenerator jg) {
    try {

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
      Set<Map.Entry<String, String>> props = event.getProperties().entrySet();
      for (Map.Entry<String, String> entry : props) {
        jg.writeStringField(entry.getKey(), entry.getValue());
      }

      if (MDC.getContext() != null) {
        Set<Map.Entry<String, String>> mdcProperties = MDC.getContext().entrySet();

        for (Map.Entry<String, String> entry : mdcProperties) {
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
    } catch (IOException ie) {
      LogLog.error("Unexpected error", ie);
    }
  }
}
