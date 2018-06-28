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

import cats.data.Validated
import cats.data.Validated.{ invalid, valid }

import scala.util.{ Failure, Success, Try }

object ValidationImplicits {

  /**
   * Extension methods for an iterable of [[cats.data.Validated]]
   */
  implicit class validatedIterableOps[L, R](coll: Iterable[Validated[L, R]]) {

    /**
     * Apply a function 'f' to all valid elements while preserving the invalid elements
     */
    def mapValid[O](f: R => O): Iterable[Validated[L, O]] =
      coll.map((x: Validated[L, R]) => x.map(f))

    /**
     * Apply a function 'f' to all invalid
     */
    def mapInvalid[O](f: L => O): Iterable[Validated[O, R]] =
      coll.map((x: Validated[L, R]) => x.leftMap(f))
  }

  /**
   * Convert an iterable of [[scala.util.Try]] into an iterable of [[cats.data.Validated]]
   *
   * @param coll
   * @tparam A
   */
  implicit class IterableOps[A](coll: Iterable[Try[A]]) {
    def toValidated() = coll.map(_.toValidated())
  }

  /**
   * Implicit method for converting [[scala.util.Try]] to [[cats.data.Validated]]
   *
   * @param t
   * @return
   */
  implicit def tryToValidated[A](t: Try[A]): Validated[Throwable, A] =
    t match {
      case Success(v) => valid(v)
      case Failure(e) => invalid(e)
    }

  /**
   * Extension method for converting [[scala.util.Try]] to [[cats.data.Validated]]
   *
   * @param t the Try
   */
  implicit class TryOps[A](t: Try[A]) {
    def toValidated(): Validated[Throwable, A] = tryToValidated(t)
  }

}
