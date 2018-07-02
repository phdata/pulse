/*
 * Copyright 2018 phData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
