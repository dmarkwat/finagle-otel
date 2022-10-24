package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.Service
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import io.opentelemetry.context.Context

import scala.util.Using

object TraceScoping extends Logging {

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
