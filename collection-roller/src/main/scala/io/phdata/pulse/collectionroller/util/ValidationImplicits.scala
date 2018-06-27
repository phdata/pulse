/* Copyright 2018 phData Inc. */

package io.phdata.pulse.collectionroller.util

import cats.data.Validated
import cats.data.Validated.{ invalid, valid }

import scala.util.{ Failure, Success, Try }

object ValidationImplicits {

  /**
   * Extendsion methods for an iterable of [[cats.data.Validated]]
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
