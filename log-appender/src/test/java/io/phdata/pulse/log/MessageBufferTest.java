package io.phdata.pulse.log;/* Copyright 2018 phData Inc. */

import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Test;

public class MessageBufferTest {

  @Test
  public void BatchMultipleEventsDontFlushWithLowNumber() {
    Integer bufferSize = 10;
    MessageBuffer buffer = new MessageBuffer();
    buffer.setBufferSize(bufferSize);

    // create fewer events than the buffersize
    int eventsCreated = bufferSize - 1;

    LoggingEvent[] events = TestUtils.generateEvents(eventsCreated);
    for (LoggingEvent event : events) {
      buffer.addEvent(event);
    }

    // Should be within time and size constraints for the queue, there are no events produced
    Assert.assertEquals(false, buffer.isFull());

  }

  @Test
  public void batchMultipleEventsFlushWhenOverSizeThreshold() {
    Integer batchSize = 10;
    MessageBuffer buffer = new MessageBuffer();
    buffer.setBufferSize(batchSize);

    // create more events than the buffersize
    int eventsCreated = batchSize + 1;

    LoggingEvent[] events = TestUtils.generateEvents(eventsCreated);
    for (LoggingEvent event : events) {
      buffer.addEvent(event);
    }

    // Size threshold is exceeded, produce all events
    Assert.assertEquals(true, buffer.isFull());
  }

  @Test
  public void getMessages() throws Exception {
    Integer flushInterval = 1000;
    Integer bufferSize = 10;
    MessageBuffer handler = new MessageBuffer();
    handler.setBufferSize(bufferSize);

    // create fewer events than the buffersize
    int eventsCreated = 100;

    LoggingEvent[] events = TestUtils.generateEvents(eventsCreated);
    for (LoggingEvent event : events) {
      handler.addEvent(event);
    }

    // time threshold is exceeded, produce all events
    Assert.assertEquals(100, handler.getMessages().length);
  }
}
