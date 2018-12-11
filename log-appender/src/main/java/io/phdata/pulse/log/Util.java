package io.phdata.pulse.log;


import org.apache.log4j.helpers.LogLog;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Util {
  public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
  public static final String YARN_LOG_DIR_SYSTEM_PROPERTY = "spark.yarn.app.container.log.dir";
  public static final String YARN_APP_ID_PROPERTY = "spark.yarn.app.id";

  public static String epochTimestampToISO8061(long ts) {
    Date date = new Date(ts);
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
    // @TODO get timezone from environment, this will use UTC always
    dateFormat.setTimeZone(UTC);
    return dateFormat.format(date);
  }

  public static boolean isSparkApplication() {
    return System.getProperty(YARN_LOG_DIR_SYSTEM_PROPERTY) != null;
  }

  public static String getApplicationId() {
    try {
      String[] segments = System.getProperty(YARN_LOG_DIR_SYSTEM_PROPERTY).split("/");
      if (segments.length == 0)
        return System.getProperty(YARN_APP_ID_PROPERTY);
      else
        return segments[segments.length - 2];
    } catch (Exception e) {
      LogLog.error("Could not set Spark application id: " + e, e);
      return "";
    }
  }

  public static String getContainerId() {
    try {
      String[] segments = System.getProperty(YARN_LOG_DIR_SYSTEM_PROPERTY).split("/");
      if (segments.length == 0)
        return "Driver";
      else {
        String[] underSplit = segments[segments.length - 1].split("_");
        // concatenate the container attempt and container number
        String value = underSplit[underSplit.length - 2] + "_" + underSplit[underSplit.length - 1];
        return value;
      }
    } catch (Exception e) {
      LogLog.error("Could not set Spark container id: " + e, e);
      return "";
    }
  }
}
