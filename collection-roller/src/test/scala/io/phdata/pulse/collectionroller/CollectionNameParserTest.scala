/* Copyright 2018 phData Inc. */

package io.phdata.pulse.collectionroller

import org.scalatest.FunSuite

class CollectionNameParserTest extends FunSuite {

  test("extract name part") {
    assertResult("my_collection")(CollectionNameParser.parseName("my_collection_1530046077"))
  }

  test("extract date part") {
    assertResult(1530046077L)(CollectionNameParser.parseTimestamp("my_collection_1530046077"))

  }
}
