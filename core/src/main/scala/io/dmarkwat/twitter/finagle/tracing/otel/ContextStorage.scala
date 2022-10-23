package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.tracing.Tracer
import com.twitter.util.logging.Logging
import io.dmarkwat.twitter.finagle.tracing.otel.ContextStorage.{ContextContainer, NoopScope, ctxKey}
import io.opentelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.{Context, Scope}

/**
 * Basic emulation of [[io.opentelemetry.context.ThreadLocalContextStorage]]
 * using finagle's [[com.twitter.finagle.context.LocalContext]].
 *
 * Bridges the finagle world with any other non-finagle Otel usages.
 */
class ContextStorage extends opentelemetry.context.ContextStorage with Logging {
  def currentOpt(): Option[ContextContainer] = Contexts.local.get(ctxKey)

  override def attach(toAttach: Context): Scope = {
    assert(toAttach != null)

    // we must be inside a finagle context boundary to attach context
    val container = currentOpt().getOrElse(
      throw new IllegalStateException(
        "misconfigured stack: can't attach the current context without a context container in finagle local context"
      )
    )
    val before = container.get
    // same context -- do nothing as it's presumed to be a stacked call
    if (before == toAttach) {
      trace("stacked attach() calls; returning no op scope")
      NoopScope
    } else {
      trace(s"attaching context ${Span.fromContext(toAttach).getSpanContext}")
      container() = toAttach
      () => {
        if (current() != toAttach) {
          warn(s"mismatched contexts when closing scope; was a context boundary crossed?: current(${Span
            .fromContext(current())
            .getSpanContext});attached(${Span.fromContext(toAttach).getSpanContext})")
        }
        trace(s"closing scope; attaching previous context ${Span.fromContext(before).getSpanContext}")
        container() = before
      }
    }
  }

  // get the current context, or null if we're outside a finagle context boundary -- null is per the interface spec
  override def current(): Context = {
    trace("current from finagle")
    currentOpt().map(_.get).orNull
  }
}

object ContextStorage extends Traced {
  val ctxKey: Contexts.local.Key[ContextContainer] = Contexts.local.newKey[ContextContainer]()

  override def let[O](context: Context, tracers: Tracer*)(f: => O): O = {
    Contexts.local.let(ctxKey, new ContextContainer(context)) {
      f
    }
  }

  override def contextOpt: Option[Context] = Contexts.local.get(ctxKey).map(_.get)

  object NoopScope extends Scope {
    override def close(): Unit = {}
  }

  final class ContextContainer(private var context: Context) {
    // current otel context as viewed by the current service stack
    def get: Context = context

    def update(ctx: Context): Unit = context = ctx
  }

}
