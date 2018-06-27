/* Copyright 2018 phData Inc. */

package io.phdata.pulse.collectionroller.util

import org.scalatest.FunSuite

import scala.util.{ Failure, Success, Try }

class ValidationImplicitsTest extends FunSuite {

  import ValidationImplicits._

  val testKeyword = "worked"

  val isValid: Try[String] = Success(testKeyword)

  val isNotValid: Try[String] = Failure(new Exception())

  val sequence = Seq(isValid, isNotValid)

  test("map over a sequence of valid values") {
    val mapped = sequence.toValidated().mapValid(x => x.toUpperCase())
    assert(mapped.exists(x => x.exists(_ == testKeyword.toUpperCase())))
  }
}
