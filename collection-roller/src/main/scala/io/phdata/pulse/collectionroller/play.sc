import cats.data.Validated
import cats.data.Validated.{invalid, valid}

import scala.util.{Failure, Success, Try}

implicit class TryOps(f: Try[_]) {
  def toValidated() = f match {
    case Success(v) => valid(v)
    case Failure(e) => invalid(e)
  }
}

implicit def tryToValidated(t: Try[_]) = {
  t match {
    case Success(v) => valid(v)
    case Failure(e) => invalid(e)
  }
}

val isValid: Try[String] = Success("worked")

val isNotValid: Try[String] = Failure(new Exception())

implicit class validatedSeqOps[L, R](coll: Seq[Validated[L, R]]) {
  def mapValid[O](f: R => O) = coll.map((x: Validated[L, R]) => x.map(f))
}

val ops: Seq[Validated[_, _]] = Seq(isValid, isNotValid)

ops.mapValid(x => x.toString)
