package info.lukaci.utils

import io.reactivex.Single

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/**
  * created by lukaci on 16/03/2019.
  */

object AsyncUtils {
  case class AtMost(value: Duration)

  implicit class ScalaSingle[T](single: Single[T]) {
    def asScala: Future[T] = {
      val prom: Promise[T] = Promise()
      single.subscribe((ok, ko) => {
        prom.complete(if(ko == null) Success(ok) else Failure(ko))
      })
      prom.future
    }

    def asUnit(implicit ec: ExecutionContext): Future[Unit] = {
      asScala map { _ => () }
    }
  }

  implicit class FutureResult[T](future: Future[T]) {
    def await(implicit atmost: AtMost): T = Await.result(future, atmost.value)
  }
}
