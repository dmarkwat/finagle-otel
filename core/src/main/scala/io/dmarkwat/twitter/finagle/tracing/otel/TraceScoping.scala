package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.{Filter, Service}
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import io.opentelemetry.context.Context

import scala.util.Using

object TraceScoping extends Logging {

  /**
   * Methods for exposing [[Context]] from finagle to external components, such as third party libs.
   *
   * This is only necessary when not using the javaagent instrumentation; however, it's harmless to
   * do so regardless and so is done for both cases to simplify code paths.
   */
  object extern {
    def makeCurrent[R](context: Context)(fn: => R): R = ContextStorage.ensure {
      trace(s"using context: $context")
      val tried = Using(context.makeCurrent()) { _ =>
        fn
      }
      trace(s"releasing context: $context")
      tried.get
    }

    def wrapping[R](context: Context)(fn: => R): () => R = () => makeCurrent(context)(fn)

    def wrapping[Req, Rep](context: Context, svc: Service[Req, Rep])(req: Req): Future[Rep] =
      ContextStorage.ensure {
        val scope = context.makeCurrent()
        trace(s"using context: $context")
        svc(req) ensure {
          trace(s"releasing context: $context")
          scope.close()
        }
      }
  }

  /**
   * Methods for exposing [[Context]] from external components, such as third party libs, to finagle.
   *
   * Certain callback mechanisms may require this, for example, and may carry with them new or altered
   * [[Context]] data.
   *
   * Even if the original object is the same as what was exposed, it's safe and therefore perhaps wise
   * to use these methods to set the [[Context]] for finagle's usage in kind.
   */
  object intern {
    def makeCurrent[R](context: Context)(fn: => R): R = Traced.get.let(context)(fn)

    def wrapping[R](context: Context)(fn: => R): () => R = () => makeCurrent(context)(fn)

    def wrapping[Req, Rep](context: Context, svc: Service[Req, Rep])(req: Req): Future[Rep] = {
      Filter.mk[Req, Rep, Req, Rep] { (req, svc) =>
        Traced.get.let(context) {
          svc(req)
        }
      } andThen svc apply (req)
    }
  }
}
