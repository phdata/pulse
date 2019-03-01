package io.phdata.pulse.logcollector

import io.phdata.pulse.common.domain.LogEvent

object Util {

  /**
   * Convert a [[LogEvent]] to [[Map[String,String]] by flattening the nested properties field
   * @param logEvent LogEvent to convert
   * @return The event, with properties flattened, as a [[Map[String,String]]
   */
  def logEventToFlattenedMap(logEvent: LogEvent): Map[String, String] = {
    val rawMap = Map(
      "id"          -> logEvent.id.getOrElse(""),
      "category"    -> logEvent.category,
      "timestamp"   -> logEvent.timestamp,
      "level"       -> logEvent.level,
      "message"     -> logEvent.message,
      "threadName"  -> logEvent.threadName,
      "throwable"   -> logEvent.throwable.getOrElse(""),
      "application" -> logEvent.application.getOrElse("")
    )

    logEvent.properties match {
      case Some(properties) => rawMap ++ properties
      case None             => rawMap
    }
  }
}
