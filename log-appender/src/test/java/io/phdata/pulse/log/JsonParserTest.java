package io.phdata.pulse.log;/* Copyright 2018 phData Inc. */

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonParserTest {
  JsonParser parser = new JsonParser();

  @Test
  public void testMarshallEventArray() throws Exception {
    LoggingEvent[] events  =TestUtils.generateEvents(10);

    String json = parser.marshallArray(events);

    assert(json != null);
    assert(json.length() > 1);
  }

  @Test
  public void marshallEvent() throws Exception {
    JsonFactory jsonFactory = new JsonFactory();

    MDC.put("hostname", "host1.com");

    // declare and initialize variables
    Logger logger = Logger.getLogger("io.phdata.pulse.log.HttpAppenderTest");
    long timeStamp = 1;
    Throwable throwable = new Throwable("Test");
    ThrowableInformation throwableInformation = new ThrowableInformation(throwable, logger);
    HashMap properties = new HashMap();
    properties.put("key", "value");

    LoggingEvent event = new LoggingEvent("io.phdata.pulse.log.HttpAppenderTest", logger, timeStamp, Level.INFO, "Hello, World",
            "main", throwableInformation, "ndc", null, properties);

    // call renderEvent method which converts an event into json
    String json = parser.marshallEvent(event);

    assertNotNull(json);

    // read json for parsing
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readValue(json, JsonNode.class);

    // check value of each field of LoggingEvent
    assertEquals("io.phdata.pulse.log.HttpAppenderTest", rootNode.get("category").asText());
    assertEquals("1970-01-01T00:00:00Z", rootNode.get("timestamp").asText());
    assertEquals("INFO", rootNode.get("level").asText());
    assertEquals("Hello, World", rootNode.get("message").asText());
    assertEquals("main", rootNode.get("threadName").asText());
    Assert.assertTrue(rootNode.get("throwable").toString().contains("java.lang.Throwable: Test"));
    assertEquals("{\"key\":\"value\",\"hostname\":\"host1.com\"}", rootNode.get("properties").toString());
  }

  @Test
  public void testNullMdc() throws Exception {
    JsonFactory jsonFactory = new JsonFactory();

    StringWriter writer = new StringWriter();
    JsonGenerator jg = jsonFactory.createGenerator(writer);

    MDC.clear();
    assert (MDC.getContext() == null);

    Logger logger = Logger.getLogger("io.phdata.pulse.log.HttpAppenderTest");
    long timeStamp = 1;
    Throwable throwable = new Throwable("Test");
    ThrowableInformation throwableInformation = new ThrowableInformation(throwable, logger);
    HashMap properties = new HashMap();
    properties.put("key", "value");

    LoggingEvent event = new LoggingEvent("io.phdata.pulse.log.HttpAppenderTest", logger, timeStamp, Level.INFO, "Hello, World",
            "main", throwableInformation, "ndc", null, properties);

    String json = parser.marshallEvent(event);

    assertNotNull(json);
  }
}
