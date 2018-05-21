package io.phdata.pulse.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;

public class HttpAppenderTest {

  @Mock
  private HttpManager httpManager;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }


  @Test
  public void testRenderJson() throws Exception {

    HttpAppender appender = new HttpAppender();

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

    // call renderJson method which converts an event into json
    String json = appender.renderJson(event);

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
  public void testNullMdc() {
    MDC.clear();
    assert (MDC.getContext() == null);
    HttpAppender appender = new HttpAppender();

    Logger logger = Logger.getLogger("io.phdata.pulse.log.HttpAppenderTest");
    long timeStamp = 1;
    Throwable throwable = new Throwable("Test");
    ThrowableInformation throwableInformation = new ThrowableInformation(throwable, logger);
    HashMap properties = new HashMap();
    properties.put("key", "value");

    LoggingEvent event = new LoggingEvent("io.phdata.pulse.log.HttpAppenderTest", logger, timeStamp, Level.INFO, "Hello, World",
            "main", throwableInformation, "ndc", null, properties);

    String json = appender.renderJson(event);

    assertNotNull(json);
  }

  @Test
  public void testStopPostingOnFailure() {
    Logger logger = Logger.getLogger("io.phdata.pulse.log.HttpAppenderTest");

    LoggingEvent event = new LoggingEvent("io.phdata.pulse.log.HttpAppenderTest", logger, 1, Level.INFO, "Hello, World",
            "main", null, "ndc", null, null);

    // there was an error
    Mockito.when(httpManager.send(Matchers.any())).thenReturn(false);
    HttpAppender appender = new HttpAppender();
    appender.setHttpManager(httpManager);
    // first event should call 'send'
    appender.append(event);
    // second event should not call 'send'
    appender.append(event);

    // verify 'send' was called only once
    Mockito.verify(httpManager, times(1)).send(Matchers.any());
  }

}

/**
 * Sample JSON event log
 * {
 * "category":"io.phdata.pulse.log.HttpAppenderTest",
 * "timestamp":1521575572598,
 * "level":"INFO",
 * "message":"Hello, World",
 * "threadName":"main",
 * "ndc":"ndc",
 * "properties":{"key":"value"},
 * "thrown":["java.lang.Throwable: Test","\tat io.phdata.pulse.log.HttpAppenderTest.testRenderJson(HttpAppenderTest.java:24)","\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)","\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)","\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)","\tat java.lang.reflect.Method.invoke(Method.java:498)","\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)","\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)","\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)","\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)","\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)","\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)","\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)","\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)","\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)","\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)","\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)","\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)","\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)","\tat org.junit.runner.JUnitCore.run(JUnitCore.java:137)","\tat com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:68)","\tat com.intellij.rt.execution.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:47)","\tat com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:242)","\tat com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:70)"]
 * }
 **/