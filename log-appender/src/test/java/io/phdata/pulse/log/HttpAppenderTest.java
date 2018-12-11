package io.phdata.pulse.log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import static org.mockito.Mockito.times;

public class HttpAppenderTest {

  private static final String SPARK_LOG_DIR = "/data1/yarn/container-logs/application_1542215373081_0707/container_1542215373081_0707_01_000002";
  private static final String APP_ID = "application_1542215373081_0707";
  private static final String CONTAINER_ID = "01_000002";

  @Mock
  private HttpManager httpManager;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void dontPostWhenBatchingHandlerIsntReady() {
    Logger logger = Logger.getLogger("io.phdata.pulse.log.HttpAppenderTest");

    LoggingEvent event = new LoggingEvent("io.phdata.pulse.log.HttpAppenderTest", logger, 1, Level.INFO, "Hello, World",
            "main", null, "ndc", null, null);

    Mockito.when(httpManager.send(Matchers.any())).thenReturn(true);
    HttpAppender appender = new HttpAppender();

    appender.setHttpManager(httpManager);
    // first event should call 'send'
    appender.append(event);

    // verify 'send' was called only once
    Mockito.verify(httpManager, times(1)).send(Matchers.any());
  }

  @Test
  public void flushEventsOnError() {
    Logger logger = Logger.getLogger("io.phdata.pulse.log.HttpAppenderTest");

    LoggingEvent event = new LoggingEvent("io.phdata.pulse.log.HttpAppenderTest", logger, 1, Level.ERROR, "Hello, World",
            "main", null, "ndc", null, null);

    Mockito.when(httpManager.send(Matchers.any())).thenReturn(true);
    HttpAppender appender = new HttpAppender();

    appender.setHttpManager(httpManager);
    // first event should call 'send'
    appender.append(event);

    // verify 'send' was called
    Mockito.verify(httpManager, times(1)).send(Matchers.any());
  }

  @Test
  public void setHostname() {
    Logger logger = Logger.getLogger("io.phdata.pulse.log.HttpAppenderTest");

    LoggingEvent event = new LoggingEvent("io.phdata.pulse.log.HttpAppenderTest", logger, 1, Level.ERROR, "Hello, World",
            "main", null, "ndc", null, null);

    ArgumentCaptor<String> sendArgument = ArgumentCaptor.forClass(String.class);

    HttpAppender appender = new HttpAppender();

    appender.setHttpManager(httpManager);
    // first event should call 'send' since we are sending an error message
    appender.append(event);

    Mockito.verify(httpManager).send(sendArgument.capture());

    assert (sendArgument.getValue().contains("\"hostname\":"));
  }

  @Test
  public void testApplicationIdAndContainerIdAppended() {
    try {
      System.setProperty(Util.YARN_LOG_DIR_SYSTEM_PROPERTY, SPARK_LOG_DIR);
      System.setProperty(Util.YARN_APP_ID_PROPERTY, APP_ID);

      Logger logger = Logger.getLogger("io.phdata.pulse.log.HttpAppenderTest");

      LoggingEvent event = new LoggingEvent("io.phdata.pulse.log.HttpAppenderTest", logger, 1, Level.ERROR, "Hello, World",
              "main", null, "ndc", null, null);

      ArgumentCaptor<String> sendArgument = ArgumentCaptor.forClass(String.class);

      HttpAppender appender = new HttpAppender();

      appender.setHttpManager(httpManager);
      // first event should call 'send' since we are sending an error message
      appender.append(event);

      Mockito.verify(httpManager).send(sendArgument.capture());

      System.out.println(sendArgument.getValue());
      Assert.assertTrue(sendArgument.getValue().contains("\"application_id\":\"application_1542215373081_0707\""));
      Assert.assertTrue(sendArgument.getValue().contains("\"container_id\":\"01_000002\""));
    } finally {
      System.clearProperty(Util.YARN_LOG_DIR_SYSTEM_PROPERTY);
      System.clearProperty(Util.YARN_APP_ID_PROPERTY);
    }
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