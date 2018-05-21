package io.phdata.pulse.log;

import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class UtilTest {
  @Test
  public void testParseTimestampToIso8601() {
    assertEquals("1970-01-01T00:00:00Z", Util.epochTimestampToISO8061(1));
  }
}
