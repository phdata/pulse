/* Copyright 2018 phData Inc. */

package io.phdata.pulse.collectionroller

object CollectionNameParser {

  /**
   * Parse out the name of a collection in the form pulse-test-options2_1530046077
   * @param collection The collection
   * @return Collection name
   */
  def parseName(collection: String): String =
    collection.split("_").dropRight(1).mkString("_")

  /**
   * Parse out the timestamp of a collection in the form pulse-test-options2_1530046077
   * @param collection The collection
   * @return Collection unix timestamp created date
   */
  def parseTimestamp(collection: String): Long = {
    val split = collection.split("_")
    split(split.size - 1).toLong
  }
}
