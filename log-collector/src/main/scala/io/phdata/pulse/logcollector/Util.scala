package io.phdata.pulse.logcollector

import io.phdata.pulse.common.domain.LogEvent

object Util {

  /**
   * Convert a [[LogEvent]] to a flattened [[Map[String,String]]
   * @param logEvent LogEvent to convert
   * @return The event, with properties flattened, as a [[Map[String,String]]
   */
  def logEventToFlattenedMap(logEvent: LogEvent): Map[String, String] = {
    val values = logEvent.productIterator
    val logEventmap = logEvent.getClass.getDeclaredFields.map {
      _.getName -> (values.next() match {
        case x => x
      })
    }.toMap
    logEventmap
      .flatten {
        case ((key, map: Option[Map[String, String]]))
            if map.getOrElse(None).isInstanceOf[Map[String, String]] =>
          map.get
        case ((key, value: Option[String])) if value.getOrElse(None).isInstanceOf[String] =>
          Map(key -> value.getOrElse(None))
        case ((key, value)) => Map(key -> value)
      }
      .toMap
      .filter(kv => !kv._2.equals(None))
      .asInstanceOf[Map[String, String]]

  }

}
