package io.phdata.pulse.log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Util {
  public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

  public static String epochTimestampToISO8061(long ts) {
    Date date = new Date(ts);
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
    // @TODO get timezone from environment, this will use UTC always
    dateFormat.setTimeZone(UTC);
    return dateFormat.format(date);
  }
}
