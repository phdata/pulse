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

package io.phdata.pulse.collectionroller.util

import org.scalatest.FunSuite

import scala.util.{ Failure, Success, Try }

class ValidationImplicitsTest extends FunSuite {

  import ValidationImplicits._

  val testKeyword = "worked"

  val success: Try[String] = Success(testKeyword)
  val failure: Try[String] = Failure(new Exception())
  val sequence             = Seq(success, failure)

  test("map over a sequence of valid values") {
    val mapped = sequence.toValidated().mapValid(x => x.toUpperCase())
    assert(mapped.exists(x => x.exists(_ == testKeyword.toUpperCase())))
  }

  test("convert at Try into a Validated") {
    assert(Try(throw new Exception).toValidated().isInvalid)
    assert(Try(1).toValidated().isValid)
  }

  test("convert an Iterable[Try] to Iterable[Validated]") {
    assert(sequence.toValidated().exists(_.isValid))
    assert(sequence.toValidated().exists(_.isInvalid))
  }
}
