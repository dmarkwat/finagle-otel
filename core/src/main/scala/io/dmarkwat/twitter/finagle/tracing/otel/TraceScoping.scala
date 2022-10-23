package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.Service
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import io.opentelemetry.context.Context

import scala.util.Using

object TraceScoping extends Logging {

  def makeCurrent[R](context: Context)(fn: => R): R = ContextStorage.let(context) {
    trace(s"using context: $context")
    val tried = Using(context.makeCurrent()) { _ =>
      fn
    }
    trace(s"releasing context: $context")
    tried.get
  }

  /**
   * Analogous to [[wrapping()]] but without using a finagle future.
   * Creates a closure around the current context and defers execution to the caller.
   *
   * @param context
   * @param fn
   * @tparam R
   * @return
   */
  def wrapping[R](context: Context)(fn: => R): () => R = () => makeCurrent(context)(fn)

  def wrapping[Req, Rep](context: Context, svc: Service[Req, Rep])(req: Req): Future[Rep] =
    ContextStorage.let(context) {
      val scope = context.makeCurrent()
      trace(s"using context: $context")
      svc(req) ensure {
        trace(s"releasing context: $context")
        scope.close()
      }
    }
}
