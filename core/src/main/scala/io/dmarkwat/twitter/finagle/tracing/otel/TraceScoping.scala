package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.Service
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import io.opentelemetry.context.Context

import scala.util.Using

object TraceScoping extends Logging {

  def makeCurrent[R](context: Context)(fn: => R): R = {
    trace(s"using context: $context")
    val tried = Using(context.makeCurrent()) { _ =>
      fn
    }
    trace(s"releasing context: $context")
    tried.get
  }

  def usingCurrent[R](fn: => R): R = {
    makeCurrent(TraceSpan.context)(fn)
  }

  def wrapping[R](fn: => R): () => R = { () =>
    usingCurrent(fn)
  }

  def wrapping[Req, Rep](svc: Service[Req, Rep])(req: Req): Future[Rep] = {
    val ctx = TraceSpan.context
    val scope = ctx.makeCurrent()
    trace(s"using context: $ctx")
    svc(req) ensure {
      trace(s"releasing context: $ctx")
      scope.close()
    }
  }
}
