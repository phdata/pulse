package io.phdata.pulse.log;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class UtilTest {

  private static final String SPARK_LOG_DIR = "/data1/yarn/container-logs/application_1542215373081_0707/container_1542215373081_0707_01_000002";
  private static final String APP_ID = "application_1542215373081_0707";
  private static final String CONTAINER_ID = "01_000002";

  @Before
  public void before() {
    System.setProperty(Util.YARN_LOG_DIR_SYSTEM_PROPERTY, SPARK_LOG_DIR);
    System.setProperty(Util.YARN_APP_ID_PROPERTY, APP_ID);
  }

  @Test
  public void testParseTimestampToIso8601() {
    assertEquals("1970-01-01T00:00:00Z", Util.epochTimestampToISO8061(1));
  }

  @Test
  public void testIsSpark() {
    assertTrue(Util.isSparkApplication());
  }

  @Test
  public void testGetApplicationId() {
    assertEquals(APP_ID, Util.getApplicationId());
  }

  @Test
  public void testGetContainerId() {
    assertEquals(CONTAINER_ID, Util.getContainerId());
  }
}
