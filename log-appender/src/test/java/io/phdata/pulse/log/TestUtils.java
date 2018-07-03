package io.phdata.pulse.log;/* Copyright 2018 phData Inc. */

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestUtils {

  private static LoggingEvent event = new LoggingEvent("io.phdata.pulse.log.HttpAppenderTest",
          Logger.getLogger("io.phdata.pulse.log.HttpAppenderTest"),
          1,
          Level.INFO,
          "Hello, World",
          "main", null, "ndc", null, null);
  @Test
  public void testEventGeneratorSize() {
    Assert.assertEquals(10, generateEvents(10).length);
  }

  public static LoggingEvent getEvent() {
    return event;
  }

  public static LoggingEvent[] generateEvents(Integer numEvents) {

    List<LoggingEvent> events = new ArrayList<>();
    for (int i = 0; i < numEvents; i++) {
      events.add(event);
    }

    return events.toArray(new LoggingEvent[]{});
  }
}
